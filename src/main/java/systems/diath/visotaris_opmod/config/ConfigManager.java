package systems.diath.visotaris_opmod.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris_opmod.VisotarisLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Lädt und speichert die Mod-Konfiguration als TOML-Datei
 * ({@code config/visotaris.toml} im Minecraft-Konfigurationsverzeichnis).
 *
 * Serialisierung ohne Reflection – jedes Feld wird explizit gelesen/geschrieben.
 * Unbekannte Schlüssel werden ignoriert; fehlende Schlüssel behalten ihren Default.
 */
public final class ConfigManager {

    private static final Gson GSON = new Gson();

    private final Path configPath;

    private VisotarisConfig config = new VisotarisConfig();

    public ConfigManager() {
        this.configPath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("visotaris.toml");
    }

    /** Nur für Unit-Tests – umgeht FabricLoader. */
    ConfigManager(VisotarisConfig cfg) {
        this.configPath = null;
        this.config = cfg;
    }

    /** Package-private file-backed constructor for migration tests. */
    ConfigManager(Path path) { this.configPath = path; }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            VisotarisLogger.info("Neue Konfiguration erstellt: {}", configPath);
            return;
        }
        boolean migrateLegacySettings = false;
        try (CommentedFileConfig toml = CommentedFileConfig.builder(configPath, TomlFormat.instance()).build()) {
            toml.load();
            migrateLegacySettings = toml.contains("modus") || toml.contains("anzeige") || toml.contains("schutz") || toml.contains("netzwerk")
                || hasLegacyFlatSettings(toml) || toml.contains("features.enableDiscordRpc")
                || toml.contains("features.discordApplicationId") || toml.contains("features.saveDiscordScreenshotsLocally")
                || toml.contains("features.verboseDiscordScreenshotLogging");
            VisotarisConfig c = new VisotarisConfig();
            // ── Modus ─────────────────────────────────────────────────────────────────
            c.observerModeOnly = bool(toml, "mode.observerModeOnly", "modus.observerModeOnly", "observerModeOnly", c.observerModeOnly);
            // ── Anzeige (neu: anzeige.x; alt: x) ─────────────────────────────────────
            c.showMarketTooltips   = bool(toml, "display.showMarketTooltips", "anzeige.showMarketTooltips", "showMarketTooltips", c.showMarketTooltips);
            c.showHud              = bool(toml, "display.showHud", "anzeige.showHud", "showHud", c.showHud);
            c.showContainerOverlay = bool(toml, "display.showContainerOverlay", "anzeige.showContainerOverlay", "showContainerOverlay", c.showContainerOverlay);
            c.showQuickButtons     = bool(toml, "display.showQuickButtons", "anzeige.showQuickButtons", "showQuickButtons", c.showQuickButtons);
            c.shulkerRecursion     = bool(toml, "display.shulkerRecursion", "anzeige.shulkerRecursion", "shulkerRecursion", c.shulkerRecursion);
            c.tooltipShowBuyPrice = bool(toml, "tooltips.showBuyPrice", "", "", c.tooltipShowBuyPrice);
            c.tooltipShowSellPrice = bool(toml, "tooltips.showSellPrice", "", "", c.tooltipShowSellPrice);
            c.tooltipShowMerchantRates = bool(toml, "tooltips.showMerchantRates", "", "", c.tooltipShowMerchantRates);
            c.tooltipShowShardRates = bool(toml, "tooltips.showShardRates", "", "", c.tooltipShowShardRates);
            c.tooltipShowDataAge = bool(toml, "tooltips.showDataAge", "", "", c.tooltipShowDataAge);
            c.tooltipShowStaleData = bool(toml, "tooltips.showStaleData", "", "", c.tooltipShowStaleData);
            c.tooltipMaxAgeSeconds = intValue(toml, "tooltips.maxAgeSeconds", c.tooltipMaxAgeSeconds);
            c.priceAlertsEnabled = bool(toml, "priceAlerts.enabled", "", "", c.priceAlertsEnabled);
            Object storedRules = toml.get("priceAlerts.rules");
            if (storedRules instanceof java.util.List<?> list) for (Object entry : list) {
                try {
                    if (entry instanceof String json) {
                        java.util.Map<?, ?> map = GSON.fromJson(JsonParser.parseString(json), java.util.Map.class);
                        PriceAlertRule rule = PriceAlertRule.fromMap(map);
                        c.priceAlertRules.add(rule);
                        if (map.get("notification") instanceof String stored && !stored.equals(rule.notification)) migrateLegacySettings = true;
                    } else if (entry instanceof java.util.Map<?, ?> map) {
                        PriceAlertRule rule = PriceAlertRule.fromMap(map);
                        c.priceAlertRules.add(rule);
                        if (map.get("notification") instanceof String stored && !stored.equals(rule.notification)) migrateLegacySettings = true;
                    }
                } catch (RuntimeException ignored) { }
            }
            // ── Schutz ────────────────────────────────────────────────────────────────
            c.enableRenameProtection = bool(toml, "protection.enableRenameProtection", "schutz.enableRenameProtection", "enableRenameProtection", c.enableRenameProtection);
            c.enableSignProtection   = bool(toml, "protection.enableSignProtection", "schutz.enableSignProtection", "enableSignProtection", c.enableSignProtection);
            c.enableOffhandBlocker   = bool(toml, "protection.enableOffhandBlocker", "schutz.enableOffhandBlocker", "enableOffhandBlocker", c.enableOffhandBlocker);
            c.enableInventoryWarning = bool(toml, "protection.enableInventoryWarning", "schutz.enableInventoryWarning", "enableInventoryWarning", c.enableInventoryWarning);
            // ── Features ──────────────────────────────────────────────────────────────
            c.enableJobTracker         = toml.getOrElse("features.enableJobTracker",         toml.getOrElse("enableJobTracker",         c.enableJobTracker));
            c.enableCommandShortforms  = toml.getOrElse("features.enableCommandShortforms",  toml.getOrElse("enableCommandShortforms",  c.enableCommandShortforms));
            c.enableAnvilNormalization = toml.getOrElse("features.enableAnvilNormalization", toml.getOrElse("enableAnvilNormalization", c.enableAnvilNormalization));
            // ── Discord (neu: discord.x; alt: features.x / x) ─────────────────────────
            c.enableDiscordRpc = toml.getOrElse(
                "discord.enableDiscordRpc",
                toml.getOrElse("features.enableDiscordRpc", toml.getOrElse("enableDiscordRpc", c.enableDiscordRpc))
            );
            c.discordApplicationId = toml.getOrElse(
                "discord.applicationId",
                toml.getOrElse("features.discordApplicationId", toml.getOrElse("discordApplicationId", c.discordApplicationId))
            );
            c.saveDiscordScreenshotsLocally = toml.getOrElse(
                "discord.saveScreenshotsLocally",
                toml.getOrElse("features.saveDiscordScreenshotsLocally", toml.getOrElse("saveDiscordScreenshotsLocally", c.saveDiscordScreenshotsLocally))
            );
            c.verboseDiscordScreenshotLogging = toml.getOrElse(
                "discord.verboseScreenshotLogging",
                toml.getOrElse("features.verboseDiscordScreenshotLogging", toml.getOrElse("verboseDiscordScreenshotLogging", c.verboseDiscordScreenshotLogging))
            );
            for (int i = 0; i < c.discordScreenshotTargets.length; i++) {
                int slot = i + 1;
                c.discordScreenshotTargets[i].enabled = toml.getOrElse(
                    "discordScreenshots.target" + slot + ".enabled",
                    toml.getOrElse("discordScreenshotTarget" + slot + "Enabled", c.discordScreenshotTargets[i].enabled)
                );
                c.discordScreenshotTargets[i].name = toml.getOrElse(
                    "discordScreenshots.target" + slot + ".name",
                    toml.getOrElse("discordScreenshotTarget" + slot + "Name", c.discordScreenshotTargets[i].name)
                );
                c.discordScreenshotTargets[i].webhookUrl = toml.getOrElse(
                    "discordScreenshots.target" + slot + ".webhookUrl",
                    toml.getOrElse("discordScreenshotTarget" + slot + "WebhookUrl", c.discordScreenshotTargets[i].webhookUrl)
                );
            }
            // ── Netzwerk ──────────────────────────────────────────────────────────────
            c.marketRefreshIntervalSeconds   = getInt(toml, "network.marketRefreshIntervalSeconds", getInt(toml, "netzwerk.marketRefreshIntervalSeconds", getInt(toml, "marketRefreshIntervalSeconds", c.marketRefreshIntervalSeconds)));
            c.merchantRefreshIntervalSeconds = getInt(toml, "network.merchantRefreshIntervalSeconds", getInt(toml, "netzwerk.merchantRefreshIntervalSeconds", getInt(toml, "merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds)));
            c.enableWebUi    = bool(toml, "network.enableWebUi", "netzwerk.enableWebUi", "enableWebUi", c.enableWebUi);
            c.webUiPort      = getInt(toml, "network.webUiPort", getInt(toml, "netzwerk.webUiPort", getInt(toml, "webUiPort", c.webUiPort)));
            c.proxyType      = normalizeProxyType(toml.getOrElse("network.proxyType", toml.getOrElse("netzwerk.proxyType", toml.getOrElse("proxyType", c.proxyType))));
            c.proxyHost      = toml.getOrElse("network.proxyHost", toml.getOrElse("netzwerk.proxyHost", toml.getOrElse("proxyHost", c.proxyHost)));
            c.proxyPort      = getInt(toml, "network.proxyPort", getInt(toml, "netzwerk.proxyPort", getInt(toml, "proxyPort", c.proxyPort)));
            c.customUserAgent = toml.getOrElse("network.customUserAgent", toml.getOrElse("netzwerk.customUserAgent", toml.getOrElse("customUserAgent", c.customUserAgent)));
            c.systemPasswordHash = toml.getOrElse("system.passwordHash", toml.getOrElse("systemPasswordHash", c.systemPasswordHash));
            config = c;
            VisotarisLogger.info("Konfiguration geladen von: {}", configPath);
        } catch (Exception e) {
            VisotarisLogger.warn("Konfiguration konnte nicht gelesen werden, nutze Defaults: {}", e.getMessage());
        }
        if (migrateLegacySettings) save();
    }

    public synchronized void save() {
        saveToDisk(config);
    }

    /** Persist a staged UI configuration and publish it to shared services only after disk success. */
    public synchronized boolean saveCandidate(VisotarisConfig candidate) {
        if (candidate == null || !saveToDisk(candidate)) return false;
        config = candidate;
        return true;
    }

    private boolean saveToDisk(VisotarisConfig c) {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            VisotarisLogger.error("Konfigurationsverzeichnis konnte nicht erstellt werden: {}", e.getMessage());
            return false;
        }
        try (CommentedFileConfig toml = CommentedFileConfig.builder(configPath, TomlFormat.instance()).build()) {
            if (Files.exists(configPath)) toml.load();
            for (String legacy : new String[]{"modus", "anzeige", "schutz", "netzwerk"}) toml.remove(legacy);
            for (String legacy : new String[]{
                "observerModeOnly", "showMarketTooltips", "showHud", "showContainerOverlay", "showQuickButtons", "shulkerRecursion",
                "enableRenameProtection", "enableSignProtection", "enableOffhandBlocker", "enableInventoryWarning",
                "enableJobTracker", "enableCommandShortforms", "enableAnvilNormalization", "enableDiscordRpc", "discordApplicationId",
                "saveDiscordScreenshotsLocally", "verboseDiscordScreenshotLogging", "marketRefreshIntervalSeconds",
                "merchantRefreshIntervalSeconds", "enableWebUi", "webUiPort", "proxyType", "proxyHost", "proxyPort",
                "customUserAgent", "systemPasswordHash"
            }) toml.remove(legacy);
            for (String legacy : new String[]{"enableDiscordRpc", "discordApplicationId", "saveDiscordScreenshotsLocally", "verboseDiscordScreenshotLogging"})
                toml.remove("features." + legacy);
            for (int slot = 1; slot <= 5; slot++) {
                toml.remove("discordScreenshotTarget" + slot + "Enabled");
                toml.remove("discordScreenshotTarget" + slot + "Name");
                toml.remove("discordScreenshotTarget" + slot + "WebhookUrl");
            }
            toml.setComment("mode", "Observer mode: data access only, no in-game interactions");
            toml.set("mode.observerModeOnly", c.observerModeOnly);
            toml.setComment("display", "HUD, tooltips and UI overlays");
            toml.set("display.showMarketTooltips", c.showMarketTooltips);
            toml.set("display.showHud", c.showHud);
            toml.set("display.showContainerOverlay", c.showContainerOverlay);
            toml.set("display.showQuickButtons", c.showQuickButtons);
            toml.set("display.shulkerRecursion", c.shulkerRecursion);
            toml.setComment("tooltips", "Client-side tooltip groups and stale-data policy");
            toml.set("tooltips.showBuyPrice", c.tooltipShowBuyPrice);
            toml.set("tooltips.showSellPrice", c.tooltipShowSellPrice);
            toml.set("tooltips.showMerchantRates", c.tooltipShowMerchantRates);
            toml.set("tooltips.showShardRates", c.tooltipShowShardRates);
            toml.set("tooltips.showDataAge", c.tooltipShowDataAge);
            toml.set("tooltips.showStaleData", c.tooltipShowStaleData);
            toml.set("tooltips.maxAgeSeconds", c.tooltipMaxAgeSeconds);
            toml.setComment("priceAlerts", "Client-side market observation only; no trades or server actions");
            toml.set("priceAlerts.enabled", c.priceAlertsEnabled);
            toml.set("priceAlerts.rules", c.priceAlertRules.stream().map(rule -> GSON.toJson(rule.toMap())).toList());
            toml.setComment("protection", "Client-side safety features");
            toml.set("protection.enableRenameProtection", c.enableRenameProtection);
            toml.set("protection.enableSignProtection", c.enableSignProtection);
            toml.set("protection.enableOffhandBlocker", c.enableOffhandBlocker);
            toml.set("protection.enableInventoryWarning", c.enableInventoryWarning);
            // ── [features] ────────────────────────────────────────────────────────────
            toml.setComment("features", "In-game gameplay features");
            toml.set("features.enableJobTracker",         c.enableJobTracker);
            toml.set("features.enableCommandShortforms",  c.enableCommandShortforms);
            toml.set("features.enableAnvilNormalization", c.enableAnvilNormalization);
            // ── [discord] ─────────────────────────────────────────────────────────────
            toml.setComment("discord", "Discord Rich Presence and screenshot delivery");
            toml.set("discord.enableDiscordRpc", c.enableDiscordRpc);
            toml.set("discord.applicationId", c.discordApplicationId);
            toml.set("discord.saveScreenshotsLocally", c.saveDiscordScreenshotsLocally);
            toml.set("discord.verboseScreenshotLogging", c.verboseDiscordScreenshotLogging);
            toml.setComment("discordScreenshots", "Discord screenshot targets; each target can be enabled independently");
            for (int i = 0; i < c.discordScreenshotTargets.length; i++) {
                int slot = i + 1;
                VisotarisConfig.DiscordScreenshotTarget target = c.discordScreenshotTargets[i];
                toml.set("discordScreenshots.target" + slot + ".enabled", target.enabled);
                toml.set("discordScreenshots.target" + slot + ".name", target.name);
                toml.set("discordScreenshots.target" + slot + ".webhookUrl", target.webhookUrl);
            }
            // ── [netzwerk] ────────────────────────────────────────────────────────────
            toml.setComment("network", "Refresh intervals, local web UI and outbound API proxy");
            toml.set("network.marketRefreshIntervalSeconds", c.marketRefreshIntervalSeconds);
            toml.set("network.merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds);
            toml.set("network.enableWebUi", c.enableWebUi);
            toml.set("network.webUiPort", c.webUiPort);
            toml.set("network.proxyType", normalizeProxyType(c.proxyType));
            toml.set("network.proxyHost", c.proxyHost);
            toml.set("network.proxyPort", c.proxyPort);
            toml.set("network.customUserAgent", c.customUserAgent);
            toml.setComment("system", "Local system access; stores only the Argon2id hash, never a plaintext password");
            toml.set("system.passwordHash", c.systemPasswordHash);
            java.io.StringWriter serialized = new java.io.StringWriter();
            TomlFormat.instance().createWriter().write(toml, serialized);
            Path temporary = Files.createTempFile(configPath.getParent(), "visotaris-", ".toml.tmp");
            try {
                Files.writeString(temporary, serialized.toString(), java.nio.charset.StandardCharsets.UTF_8);
                try {
                    Set<java.nio.file.attribute.PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
                    Files.setPosixFilePermissions(temporary, permissions);
                } catch (UnsupportedOperationException ignored) { /* Windows ACL inheritance */ }
                try { Files.move(temporary, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
            return true;
        } catch (Exception e) {
            VisotarisLogger.error("Konfiguration konnte nicht gespeichert werden: {}", e.toString());
            return false;
        }
    }

    /** Löscht die Konfigdatei und schreibt Defaults neu (Reset-Funktion). */
    public void reset() {
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            VisotarisLogger.error("Konfigdatei konnte nicht gelöscht werden: {}", e.getMessage());
        }
        config = new VisotarisConfig();
        save();
        VisotarisLogger.info("Konfiguration zurückgesetzt.");
    }

    public VisotarisConfig getConfig() {
        return config;
    }

    /** Pfad der zentralen TOML-Konfiguration; für die lokale Auth-Sicherung. */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Liest einen Integer-Wert aus der TOML-Map.
     * night-config parst TOML-Integers intern als {@code Long} – ein direktes
     * {@code getOrElse(key, intDefault)} würde eine {@link ClassCastException}
     * werfen, sobald der Schlüssel vorhanden ist.  Dieser Helper casten daher
     * sicher über {@link Number#intValue()}.
     */
    private static int getInt(CommentedFileConfig toml, String key, int def) {
        Object val = toml.get(key);
        return val instanceof Number n ? n.intValue() : def;
    }

    private static boolean bool(CommentedFileConfig toml, String current, String legacyGroup, String legacyRoot, boolean def) {
        Object value = !current.isEmpty() ? toml.get(current) : null;
        if (!(value instanceof Boolean) && !legacyGroup.isEmpty()) value = toml.get(legacyGroup);
        if (!(value instanceof Boolean) && !legacyRoot.isEmpty()) value = toml.get(legacyRoot);
        return value instanceof Boolean b ? b : def;
    }

    private static int intValue(CommentedFileConfig toml, String key, int def) {
        Object value = toml.get(key);
        return value instanceof Number n ? n.intValue() : def;
    }

    private static boolean hasLegacyFlatSettings(CommentedFileConfig toml) {
        for (String key : new String[]{"observerModeOnly", "showMarketTooltips", "showHud", "showContainerOverlay", "showQuickButtons",
            "shulkerRecursion", "enableRenameProtection", "enableSignProtection", "enableOffhandBlocker", "enableInventoryWarning",
            "enableJobTracker", "enableCommandShortforms", "enableAnvilNormalization", "enableDiscordRpc", "discordApplicationId",
            "saveDiscordScreenshotsLocally", "verboseDiscordScreenshotLogging", "marketRefreshIntervalSeconds",
            "merchantRefreshIntervalSeconds", "enableWebUi", "webUiPort", "proxyType", "proxyHost", "proxyPort",
            "customUserAgent", "systemPasswordHash"}) if (toml.contains(key)) return true;
        for (int slot = 1; slot <= 5; slot++) if (toml.contains("discordScreenshotTarget" + slot + "Enabled")
            || toml.contains("discordScreenshotTarget" + slot + "Name") || toml.contains("discordScreenshotTarget" + slot + "WebhookUrl")) return true;
        return false;
    }

    private static String normalizeProxyType(String value) {
        if (value == null) return "HTTP";
        String normalized = value.strip().toUpperCase();
        return switch (normalized) {
            case "HTTPS", "SOCKS" -> normalized;
            default -> "HTTP";
        };
    }
}
