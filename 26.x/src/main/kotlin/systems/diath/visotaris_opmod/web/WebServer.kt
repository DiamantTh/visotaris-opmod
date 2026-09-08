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
import net.minecraft.client.Minecraft
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import systems.diath.visotaris_opmod.VisotarisLogger
import systems.diath.visotaris_opmod.cache.MarketCache
import systems.diath.visotaris_opmod.cache.PriceHistoryCache
import systems.diath.visotaris_opmod.cache.ShardCache
import systems.diath.visotaris_opmod.config.ConfigManager
import java.util.Arrays

/**
 * MC 26.x – Mojang-Klassen: Minecraft (statt MinecraftClient), Identifier für Ressourcenpfade.
 *
 * Eingebetteter HTTP-Server für das Visotaris Web-UI.
 * Läuft auf localhost:[port] (Standard: 7780).
 * Alle Anfragen bleiben lokal – es werden keine Daten an externe Dienste gesendet.
 */
class WebServer(
    val port: Int,
    private val marketCache: MarketCache,
    private val shardCache: ShardCache,
    private val historyCache: PriceHistoryCache,
    private val config: ConfigManager
) {
    private val gson = Gson()
    private val systemAccess = SystemAccessControl(config)
    private val servers = mutableListOf<EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>>()
    var lastFailureReason: String = ""
        private set

    fun start() {
        if (servers.isNotEmpty()) return
        lastFailureReason = ""
        val failures = mutableListOf<String>()

        for (host in listOf("::1", "127.0.0.1")) {
            try {
                servers += buildServer(host).start(wait = false)
            } catch (e: Exception) {
                val friendly = friendlyFailure(host, e)
                failures += friendly
                VisotarisLogger.warn("Web-UI Startproblem: {}", friendly)
                VisotarisLogger.debug("Web-UI Rohfehler für {}:{}: {}", host, port, e.toString())
            }
        }

        if (servers.isEmpty()) {
            lastFailureReason = failures.joinToString("; ").ifBlank { "Kein lokaler Listener konnte gestartet werden." }
            VisotarisLogger.error(
                "Web-UI Start fehlgeschlagen: {}. Prüfe, ob Port {} bereits belegt ist oder lokale Netzwerk-Bindings blockiert werden.",
                lastFailureReason,
                port
            )
            return
        }

        VisotarisLogger.info("Web-UI gestartet: http://[::1]:{}/ | http://127.0.0.1:{}/", port, port)
    }

    fun isRunning(): Boolean = servers.isNotEmpty()

    fun stop() {
        if (servers.isEmpty()) {
            VisotarisLogger.debug("Web-UI stop() aufgerufen, war aber nicht aktiv - nichts zu tun.")
            return
        }
        servers.forEach { it.stop(gracePeriodMillis = 200, timeoutMillis = 1_000) }
        servers.clear()
        VisotarisLogger.info("Web-UI gestoppt.")
    }

    private fun friendlyFailure(host: String, error: Throwable): String {
        val root = rootCause(error)
        val message = root.message ?: error.message ?: root.javaClass.simpleName
        return when {
            root.javaClass.name.contains("BindException") || message.contains("Address already in use", ignoreCase = true) ->
                "$host:$port ist belegt (Port wird bereits genutzt)"
            message.contains("Permission denied", ignoreCase = true) ->
                "$host:$port darf nicht geöffnet werden (Berechtigung oder Firewall)"
            else ->
                "$host:$port konnte nicht geöffnet werden ($message)"
        }
    }

    private tailrec fun rootCause(error: Throwable): Throwable {
        val cause = error.cause
        return if (cause == null || cause === error) error else rootCause(cause)
    }

    private fun buildServer(host: String) = embeddedServer(CIO, port = port, host = host) {
        routing {
            // ── HTML-Seiten ─────────────────────────────────────────────────────
            get("/") { serveResource(call, "assets/webui/index.html", ContentType.Text.Html) }
            get("/history") { serveResource(call, "assets/webui/history.html", ContentType.Text.Html) }
            get("/shard") { serveResource(call, "assets/webui/shard.html", ContentType.Text.Html) }
            get("/redcoins") { serveResource(call, "assets/webui/redcoins.html", ContentType.Text.Html) }
            get("/merchant") { serveResource(call, "assets/webui/merchant.html", ContentType.Text.Html) }
            // Statischer lokaler Einrichtungs-/Login-Dialog; die Datenrouten sind geschützt.
            get("/system") { serveResource(call, "assets/webui/system.html", ContentType.Text.Html) }
            get("/system/mcinfo") {
                if (!systemAccess.authenticated(call.request.cookies[SystemAccessControl.COOKIE])) { call.respondRedirect("/system"); return@get }
                serveResource(call, "assets/webui/system.html", ContentType.Text.Html)
            }

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
                val history = if (call.request.queryParameters["refresh"] == "true") {
                    historyCache.refresh(key)
                } else {
                    historyCache.get(key)
                }
                call.respondText(gson.toJson(history), ContentType.Application.Json)
            }
            get("/api/shard") {
                call.respondText(gson.toJson(merchantRatesFor("opshards")), ContentType.Application.Json)
            }
            get("/api/redcoins") {
                call.respondText(gson.toJson(merchantRatesFor("redcoins")), ContentType.Application.Json)
            }
            get("/api/merchant") {
                call.respondText(gson.toJson(merchantRatesByTarget()), ContentType.Application.Json)
            }
            get("/api/merchant/{target}") {
                val target = call.parameters["target"]?.lowercase()?.trim().orEmpty()
                if (target.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "target fehlt")
                    return@get
                }
                call.respondText(gson.toJson(merchantRatesFor(target)), ContentType.Application.Json)
            }
            get("/api/meta") {
                call.respondText(gson.toJson(mapOf(
                    "market" to cacheMeta(marketCache.getLastUpdatedMs(), marketCache.getAgeSeconds()),
                    "merchant" to cacheMeta(shardCache.getLastUpdatedMs(), shardCache.getAgeSeconds())
                )), ContentType.Application.Json)
            }
            post("/api/system/setup") {
                if (systemAccess.configured()) { call.respond(HttpStatusCode.Conflict, "Passwort bereits eingerichtet"); return@post }
                val form = call.receiveParameters()
                val password = form["password"]?.toCharArray() ?: charArrayOf()
                val confirmation = form["confirmation"]?.toCharArray() ?: charArrayOf()
                try {
                    if (!password.contentEquals(confirmation)) { call.respond(HttpStatusCode.BadRequest, "Passwörter stimmen nicht überein"); return@post }
                    val token = systemAccess.setup(password); setSystemCookie(call, token)
                    call.respondText("{}", ContentType.Application.Json)
                } catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.BadRequest, e.message ?: "Ungültiges Passwort") }
                finally { Arrays.fill(password, '\u0000'); Arrays.fill(confirmation, '\u0000') }
            }
            post("/api/system/login") {
                if (!systemAccess.configured()) { call.respond(HttpStatusCode(428, "Precondition Required"), "Systempasswort zuerst einrichten"); return@post }
                val password = call.receiveParameters()["password"]?.toCharArray() ?: charArrayOf()
                try {
                    val token = systemAccess.login(password)
                    if (token == null) call.respond(HttpStatusCode.Unauthorized, "Anmeldung fehlgeschlagen oder kurzzeitig gesperrt")
                    else { setSystemCookie(call, token); call.respondText("{}", ContentType.Application.Json) }
                } finally { Arrays.fill(password, '\u0000') }
            }
            post("/api/system/logout") { systemAccess.logout(call.request.cookies[SystemAccessControl.COOKIE]); clearSystemCookie(call); call.respond(HttpStatusCode.NoContent) }
            get("/api/system/status") { call.respondText(gson.toJson(mapOf("configured" to systemAccess.configured())), ContentType.Application.Json) }
            get("/api/system") {
                if (!requireSystemAccess(call)) return@get
                call.respondText(gson.toJson(systemSnapshot()), ContentType.Application.Json)
            }
            get("/api/system/mcinfo") {
                if (!requireSystemAccess(call)) return@get
                call.respondText(gson.toJson(minecraftSnapshot()), ContentType.Application.Json)
            }

            // ── Item-Icons aus dem MC-ResourceManager ────────────────────────────
            get("/api/icon/{material}") {
                val referer = call.request.header("Referer") ?: ""
                val localPrefixes = listOf("http://localhost:", "http://127.0.0.1:", "http://[::1]:")
                if (localPrefixes.none { referer.startsWith(it) }) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }

                val rawKey = call.parameters["material"]?.lowercase() ?: run {
                    call.respond(HttpStatusCode.BadRequest); return@get
                }

                val rm = Minecraft.getInstance()?.resourceManager
                    ?: run { call.respond(HttpStatusCode.ServiceUnavailable); return@get }

                // Zuerst versuchen mit Original-Key (auch Custom Items wie "paper#626")
                var bytes = loadItemIconBytes(rm, rawKey)
                if (bytes != null) {
                    call.respondBytes(bytes, ContentType.Image.PNG)
                    return@get
                }

                // Fallback: Custom Item? (paper#626 → paper)
                val key = rawKey
                    .substringBefore('#')
                    .filter { it.isLetterOrDigit() || it == '_' }
                    .takeIf { !it.isNullOrBlank() }

                if (key != null) {
                    bytes = loadItemIconBytes(rm, key)
                    if (bytes != null) {
                        call.respondBytes(bytes, ContentType.Image.PNG)
                        return@get
                    }

                    // Zweiter Fallback: Custom-Item-Mapping (paper#626 → amethyst_shard, etc.)
                    val fallbackKey = getCustomItemFallback(rawKey, key)
                    if (fallbackKey != null) {
                        bytes = loadItemIconBytes(rm, fallbackKey)
                        if (bytes != null) {
                            call.respondBytes(bytes, ContentType.Image.PNG)
                            return@get
                        }
                    }
                }

                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    /** Filtert die gemeinsame Merchant-API nach Zielwährung für getrennte Web-Ansichten. */
    private fun merchantRatesFor(target: String) = shardCache.snapshot().values
        .filter { target.equals(it.target, ignoreCase = true) }
        .sortedBy { it.source }

    /** Vollständige, erweiterbare Merchant-Übersicht für neue API-Zielwährungen. */
    private fun merchantRatesByTarget() = shardCache.snapshot().values
        .groupBy { it.target?.lowercase() ?: "unknown" }
        .toSortedMap()
        .mapValues { (_, rates) -> rates.sortedBy { it.source } }

    private fun cacheMeta(updatedAtMs: Long, ageSeconds: Long) = mapOf(
        "updatedAtMs" to updatedAtMs,
        "ageSeconds" to if (ageSeconds == Long.MAX_VALUE) null else ageSeconds,
        "stale" to (ageSeconds > 300)
    )

    private suspend fun requireSystemAccess(call: ApplicationCall): Boolean {
        if (!systemAccess.configured()) { call.respond(HttpStatusCode(428, "Precondition Required"), "Systempasswort muss zuerst lokal eingerichtet werden"); return false }
        if (!systemAccess.authenticated(call.request.cookies[SystemAccessControl.COOKIE])) { call.respond(HttpStatusCode.Unauthorized, "Systemanmeldung erforderlich"); return false }
        return true
    }
    private fun setSystemCookie(call: ApplicationCall, token: String) {
        call.response.cookies.append(Cookie(SystemAccessControl.COOKIE, token, maxAge = SystemAccessControl.SESSION_SECONDS.toInt(), path = "/", httpOnly = true, extensions = mapOf("SameSite" to "Strict")))
    }
    private fun clearSystemCookie(call: ApplicationCall) {
        call.response.cookies.append(Cookie(SystemAccessControl.COOKIE, "", maxAge = 0, path = "/", httpOnly = true, extensions = mapOf("SameSite" to "Strict")))
    }

    /** Keine Identifikatoren, Pfade, Proxy- oder Webhook-Daten an das Web-UI geben. */
    private fun systemSnapshot(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val cfg = config.config
        val modVersion = FabricLoader.getInstance().getModContainer("visotaris_opmod")
            .map { it.metadata.version.friendlyString }.orElse("?")
        return mapOf(
            "application" to mapOf("modVersion" to modVersion, "webUiPort" to port),
            "runtime" to mapOf(
                "os" to System.getProperty("os.name", "?"),
                "osVersion" to System.getProperty("os.version", "?"),
                "architecture" to System.getProperty("os.arch", "?"),
                "java" to System.getProperty("java.version", "?"),
                "javaVendor" to System.getProperty("java.vendor", "?"),
                "processors" to runtime.availableProcessors(),
                "heapMaxBytes" to runtime.maxMemory(),
                "heapUsedBytes" to runtime.totalMemory() - runtime.freeMemory()
            ),
            "options" to mapOf(
                "observerMode" to cfg.observerModeOnly,
                "marketTooltips" to cfg.showMarketTooltips,
                "hud" to cfg.showHud,
                "containerOverlay" to cfg.showContainerOverlay,
                "quickButtons" to cfg.showQuickButtons,
                "jobTracker" to cfg.enableJobTracker,
                "commandShortforms" to cfg.enableCommandShortforms,
                "anvilNormalization" to cfg.enableAnvilNormalization,
                "discordRpc" to cfg.enableDiscordRpc,
                "marketRefreshSeconds" to cfg.marketRefreshIntervalSeconds,
                "merchantRefreshSeconds" to cfg.merchantRefreshIntervalSeconds
            )
        )
    }

    /** F3-nahe Clientdaten, bewusst ausschließlich hinter der System-Sitzung. */
    private fun minecraftSnapshot(): Map<String, Any?> {
        val client = Minecraft.getInstance()
        val player = client.player
        val level = client.level
        return mapOf(
            "available" to (player != null && level != null),
            "player" to player?.name?.string,
            "coordinates" to if (player == null) null else mapOf("x" to player.x, "y" to player.y, "z" to player.z,
                "blockX" to player.blockX, "blockY" to player.blockY, "blockZ" to player.blockZ),
            "dimension" to level?.dimension()?.toString(),
            "server" to client.currentServer?.ip,
            "singleplayer" to client.hasSingleplayerServer()
        )
    }

    /** Versucht, das Icon-PNG für ein Item zu laden.
     *  MC 26.x: ResourceManager.getResource(Identifier) → Optional<Resource> → Resource.open()
     */
    private fun loadItemIconBytes(rm: net.minecraft.server.packs.resources.ResourceManager, key: String): ByteArray? {
        for (prefix in listOf("item", "block")) {
            runCatching {
                return rm.getResource(Identifier.fromNamespaceAndPath("minecraft", "textures/$prefix/$key.png"))
                    .orElseThrow().open().use { it.readBytes() }
            }
        }
        for (modelType in listOf("item", "block")) {
            runCatching {
                val model = rm.getResource(Identifier.fromNamespaceAndPath("minecraft", "models/$modelType/$key.json"))
                    .orElseThrow().open().use { JsonParser.parseReader(it.reader()).asJsonObject }
                val texPath = resolveTextureInModel(rm, model, 0) ?: return@runCatching
                return rm.getResource(Identifier.fromNamespaceAndPath("minecraft", "textures/$texPath.png"))
                    .orElseThrow().open().use { it.readBytes() }
            }
        }
        return null
    }

    /** Extrahiert den ersten konkreten Texturpfad aus einem Model-JSON. */
    private fun resolveTextureInModel(
        rm: net.minecraft.server.packs.resources.ResourceManager,
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
            val parentModel = rm.getResource(Identifier.fromNamespaceAndPath("minecraft", "models/$parentPath.json"))
                .orElseThrow().open().use { JsonParser.parseReader(it.reader()).asJsonObject }
            resolveTextureInModel(rm, parentModel, depth + 1)
        }.getOrNull()
    }

    /** Fallback-Icon für bekannte Custom Items.
     *  Nutzt visotaris-spezifische Custom-ModelData-Items und mappt sie auf bessere Icons.
     *  Daten aus https://api.opsucht.net/merchant/rates
     */
    private fun getCustomItemFallback(rawKey: String, baseKey: String): String? {
        if (baseKey != "paper") return null  // Nur für Paper-basierte Custom Items

        // Custom ModelData → Fallback Icon Mapping (OPSUCHT Shardhändler Items)
        val customModelData = rawKey.substringAfter('#').takeIf { it.isNotEmpty() }?.toIntOrNull()
        return when (customModelData) {
            625 -> "stick"                 // Holzbündel - braunes Icon
            626 -> "amethyst_shard"        // Gräbergemisch - violettes Icon
            635 -> "stone"                 // Steinplatten - graues Icon
            else -> null
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
