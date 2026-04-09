package systems.diath.visotaris_opmod.web

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import systems.diath.visotaris_opmod.VisotarisLogger
import systems.diath.visotaris_opmod.cache.MarketCache
import systems.diath.visotaris_opmod.cache.PriceHistoryCache
import systems.diath.visotaris_opmod.cache.ShardCache

/**
 * Eingebetteter HTTP-Server für das Visotaris Web-UI.
 *
 * Läuft auf localhost:[port] (Standard: 7780).
 * Alle Anfragen bleiben lokal – es werden keine Daten an externe Dienste gesendet.
 *
 * Routen:
 *  GET /              → index.html (Marktpreise)
 *  GET /history       → history.html (Preisverlauf-Charts)
 *  GET /shard         → shard.html (Shardkurse)
 *  GET /static/...    → statische Dateien aus JAR-Classpath
 *  GET /api/market    → JSON: alle Marktpreise aus MarketCache
 *  GET /api/market/{material} → JSON: einzelner Marktpreis
 *  GET /api/history/{material}→ JSON: Preisverlauf (lazy fetch)
 *  GET /api/shard     → JSON: alle Shardkurse aus ShardCache
 */
class WebServer(
    private val port: Int,
    private val marketCache: MarketCache,
    private val shardCache: ShardCache,
    private val historyCache: PriceHistoryCache
) {
    private val gson = Gson()
    private val servers = mutableListOf<EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>>()

    fun start() {
        if (servers.isNotEmpty()) return

        // Beide Stacks starten – IPv6 zuerst (moderner Standard), IPv4 für "localhost"-Kompatibilität
        for (host in listOf("::1", "127.0.0.1")) {
            try {
                servers += buildServer(host).start(wait = false)
            } catch (e: Exception) {
                VisotarisLogger.warn("Web-UI konnte nicht auf {}:{} starten: {}", host, port, e.message)
            }
        }

        if (servers.isEmpty()) {
            VisotarisLogger.error("Web-UI konnte auf keiner Adresse starten.")
            return
        }

        VisotarisLogger.info("Web-UI gestartet: http://[::1]:{}/ | http://127.0.0.1:{}/", port, port)
    }

    fun stop() {
        servers.forEach { it.stop(gracePeriodMillis = 200, timeoutMillis = 1_000) }
        servers.clear()
        VisotarisLogger.info("Web-UI gestoppt.")
    }

    private fun buildServer(host: String) = embeddedServer(CIO, port = port, host = host) {
        routing {
            // ── HTML-Seiten ─────────────────────────────────────────────────────
            get("/") { serveResource(call, "assets/webui/index.html", ContentType.Text.Html) }
            get("/history") { serveResource(call, "assets/webui/history.html", ContentType.Text.Html) }
            get("/shard") { serveResource(call, "assets/webui/shard.html", ContentType.Text.Html) }

            // ── Statische Dateien ────────────────────────────────────────────────
            get("/static/{path...}") {
                val path = call.parameters.getAll("path")?.joinToString("/") ?: run {
                    call.respond(HttpStatusCode.NotFound); return@get
                }
                val contentType = when {
                    path.endsWith(".css")   -> ContentType.Text.CSS
                    path.endsWith(".js")    -> ContentType.Application.JavaScript
                    path.endsWith(".svg")   -> ContentType.Image.SVG
                    path.endsWith(".png")   -> ContentType.Image.PNG
                    path.endsWith(".ico")   -> ContentType.parse("image/x-icon")
                    path.endsWith(".woff2") -> ContentType.parse("font/woff2")
                    path.endsWith(".woff")  -> ContentType.parse("font/woff")
                    else                    -> ContentType.Application.OctetStream
                }
                serveResource(call, "assets/webui/static/$path", contentType)
            }

            // ── JSON-API ─────────────────────────────────────────────────────────
            get("/api/market") {
                call.respondText(gson.toJson(marketCache.snapshot()), ContentType.Application.Json)
            }
            get("/api/market/{material}") {
                val key = call.parameters["material"]?.lowercase() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "material fehlt"); return@get
                }
                val price = marketCache.get(key)
                if (price != null) {
                    call.respondText(gson.toJson(price), ContentType.Application.Json)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Material nicht im Cache")
                }
            }
            get("/api/history/{material}") {
                val key = call.parameters["material"]?.lowercase() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "material fehlt"); return@get
                }
                val history = historyCache.get(key)
                call.respondText(gson.toJson(history), ContentType.Application.Json)
            }
            get("/api/shard") {
                call.respondText(gson.toJson(shardCache.snapshot()), ContentType.Application.Json)
            }

            // ── Item-Icons aus dem MC-ResourceManager ────────────────────────────
            get("/api/icon/{material}") {
                // Nur Anfragen die von einer lokalen Seite stammen (kein Direktaufruf)
                val referer = call.request.header("Referer") ?: ""
                val localPrefixes = listOf("http://localhost:", "http://127.0.0.1:", "http://[::1]:")
                if (localPrefixes.none { referer.startsWith(it) }) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }

                val key = call.parameters["material"]
                    ?.lowercase()
                    ?.filter { it.isLetterOrDigit() || it == '_' }
                    .takeIf { !it.isNullOrBlank() }
                    ?: run { call.respond(HttpStatusCode.BadRequest); return@get }

                val rm = MinecraftClient.getInstance()?.resourceManager
                    ?: run { call.respond(HttpStatusCode.ServiceUnavailable); return@get }

                val bytes = loadItemIconBytes(rm, key)
                if (bytes != null) {
                    call.respondBytes(bytes, ContentType.Image.PNG)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }

    /** Versucht, das Icon-PNG für ein Item zu laden:
     *  1. textures/item/{key}.png
     *  2. textures/block/{key}.png
     *  3. models/item/{key}.json → Textur-Referenz auflösen (folgt parent bis Tiefe 3)
     *  4. models/block/{key}.json → gleiches Verfahren
     */
    private fun loadItemIconBytes(rm: net.minecraft.resource.ResourceManager, key: String): ByteArray? {
        for (prefix in listOf("item", "block")) {
            runCatching {
                return rm.open(Identifier.of("minecraft", "textures/$prefix/$key.png")).use { it.readBytes() }
            }
        }
        for (modelType in listOf("item", "block")) {
            runCatching {
                val model = rm.open(Identifier.of("minecraft", "models/$modelType/$key.json"))
                    .use { JsonParser.parseReader(it.reader()).asJsonObject }
                val texPath = resolveTextureInModel(rm, model, 0) ?: return@runCatching
                return rm.open(Identifier.of("minecraft", "textures/$texPath.png")).use { it.readBytes() }
            }
        }
        return null
    }

    /** Extrahiert den ersten konkreten Texturpfad aus einem Model-JSON.
     *  Folgt bei Bedarf dem parent-Feld rekursiv (max. Tiefe 3).
     */
    private fun resolveTextureInModel(
        rm: net.minecraft.resource.ResourceManager,
        model: JsonObject,
        depth: Int
    ): String? {
        if (depth > 3) return null
        model.getAsJsonObject("textures")?.let { textures ->
            for (key in listOf("layer0", "all", "cross", "top", "particle")) {
                val v = textures.get(key)?.asString?.takeIf { !it.startsWith("#") } ?: continue
                return if (v.contains(":")) v.substringAfter(":") else v
            }
            textures.entrySet()
                .firstOrNull { !it.value.asString.startsWith("#") }
                ?.value?.asString
                ?.let { v -> return if (v.contains(":")) v.substringAfter(":") else v }
        }
        val parent = model.get("parent")?.asString ?: return null
        val parentPath = if (parent.contains(":")) parent.substringAfter(":") else parent
        return runCatching {
            val parentModel = rm.open(Identifier.of("minecraft", "models/$parentPath.json"))
                .use { JsonParser.parseReader(it.reader()).asJsonObject }
            resolveTextureInModel(rm, parentModel, depth + 1)
        }.getOrNull()
    }

    private suspend fun serveResource(call: ApplicationCall, resourcePath: String, contentType: ContentType) {
        val stream = WebServer::class.java.classLoader.getResourceAsStream(resourcePath)
        if (stream == null) {
            call.respond(HttpStatusCode.NotFound, "Ressource nicht gefunden: $resourcePath")
            return
        }
        val bytes = stream.use { it.readBytes() }
        call.respondBytes(bytes, contentType)
    }
}
