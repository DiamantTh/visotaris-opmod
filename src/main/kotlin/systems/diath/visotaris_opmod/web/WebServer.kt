package systems.diath.visotaris_opmod.web

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import systems.diath.visotaris_opmod.VisotarisLogger
import systems.diath.visotaris_opmod.cache.MarketCache
import systems.diath.visotaris_opmod.cache.PriceHistoryCache
import systems.diath.visotaris_opmod.cache.ShardCache

/**
 * Eingebetteter HTTP-Server für das Visotaris Web-UI.
 *
 * Läuft auf localhost:[port] (Standard: 7865).
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

        // IPv4 (127.0.0.1) – erforderlich
        try {
            servers += buildServer("127.0.0.1").start(wait = false)
        } catch (e: Exception) {
            VisotarisLogger.error("Web-UI konnte nicht auf 127.0.0.1:{} starten: {}", port, e.message)
            return
        }

        // IPv6 (::1) – optional; auf Systemen ohne IPv6 überspringen
        try {
            servers += buildServer("::1").start(wait = false)
        } catch (e: Exception) {
            VisotarisLogger.warn("Web-UI IPv6 (::1:{}) nicht verfügbar: {}", port, e.message)
        }

        val v6 = if (servers.size > 1) " | http://[::1]:$port/" else ""
        VisotarisLogger.info("Web-UI gestartet: http://127.0.0.1:{}/{}",  port, v6)
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
        }
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
