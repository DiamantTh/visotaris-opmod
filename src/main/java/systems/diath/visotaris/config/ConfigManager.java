package systems.diath.visotaris.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris.VisotarisLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lädt und speichert die Mod-Konfiguration als TOML-Datei
 * ({@code config/visotaris.toml} im Minecraft-Konfigurationsverzeichnis).
 *
 * Serialisierung ohne Reflection – jedes Feld wird explizit gelesen/geschrieben.
 * Unbekannte Schlüssel werden ignoriert; fehlende Schlüssel behalten ihren Default.
 */
public final class ConfigManager {

    private final Path configPath = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("visotaris.toml");

    private VisotarisConfig config = new VisotarisConfig();

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            VisotarisLogger.info("Neue Konfiguration erstellt: {}", configPath);
            return;
        }
        try (CommentedFileConfig toml = CommentedFileConfig.builder(configPath, TomlFormat.instance()).build()) {
            toml.load();
            VisotarisConfig c = new VisotarisConfig();
            c.showMarketTooltips             = toml.getOrElse("showMarketTooltips",             c.showMarketTooltips);
            c.showHud                        = toml.getOrElse("showHud",                        c.showHud);
            c.enableJobTracker               = toml.getOrElse("enableJobTracker",               c.enableJobTracker);
            c.enableRenameProtection         = toml.getOrElse("enableRenameProtection",         c.enableRenameProtection);
            c.enableSignProtection           = toml.getOrElse("enableSignProtection",           c.enableSignProtection);
            c.enableOffhandBlocker           = toml.getOrElse("enableOffhandBlocker",           c.enableOffhandBlocker);
            c.enableInventoryWarning         = toml.getOrElse("enableInventoryWarning",         c.enableInventoryWarning);
            c.enableCommandShortforms        = toml.getOrElse("enableCommandShortforms",        c.enableCommandShortforms);
            c.marketRefreshIntervalSeconds   = getInt(toml, "marketRefreshIntervalSeconds",   c.marketRefreshIntervalSeconds);
            c.merchantRefreshIntervalSeconds = getInt(toml, "merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds);
            c.showContainerOverlay           = toml.getOrElse("showContainerOverlay",           c.showContainerOverlay);
            c.showQuickButtons               = toml.getOrElse("showQuickButtons",               c.showQuickButtons);
            c.shulkerRecursion               = toml.getOrElse("shulkerRecursion",               c.shulkerRecursion);
            c.enableAnvilNormalization       = toml.getOrElse("enableAnvilNormalization",       c.enableAnvilNormalization);
            c.enableDiscordRpc               = toml.getOrElse("enableDiscordRpc",               c.enableDiscordRpc);
            c.proxyHost                      = toml.getOrElse("proxyHost",                      c.proxyHost);
            c.proxyPort                      = getInt(toml, "proxyPort",                      c.proxyPort);
            c.apiKey                         = toml.getOrElse("apiKey",                         c.apiKey);
            c.customUserAgent                = toml.getOrElse("customUserAgent",                c.customUserAgent);
            config = c;
            VisotarisLogger.info("Konfiguration geladen von: {}", configPath);
        } catch (Exception e) {
            VisotarisLogger.warn("Konfiguration konnte nicht gelesen werden, nutze Defaults: {}", e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            VisotarisLogger.error("Konfigurationsverzeichnis konnte nicht erstellt werden: {}", e.getMessage());
            return;
        }
        try (CommentedFileConfig toml = CommentedFileConfig.builder(configPath, TomlFormat.instance()).build()) {
            VisotarisConfig c = config;
            // ── Allgemein / Anzeige ────────────────────────────────────────────────────
            toml.setComment("showMarketTooltips",
                " ── Allgemein / Anzeige ──────────────────────────────────────────────────────");
            toml.set("showMarketTooltips",             c.showMarketTooltips);
            toml.set("showHud",                        c.showHud);
            // ── Job-Tracker ─────────────────────────────────────────────────────────────
            toml.setComment("enableJobTracker",
                " ── Job-Tracker ─────────────────────────────────────────────────────────────");
            toml.set("enableJobTracker",               c.enableJobTracker);
            // ── Schutz ───────────────────────────────────────────────────────────────────
            toml.setComment("enableRenameProtection",
                " ── Schutz ───────────────────────────────────────────────────────────────────");
            toml.set("enableRenameProtection",         c.enableRenameProtection);
            toml.set("enableSignProtection",           c.enableSignProtection);
            toml.set("enableOffhandBlocker",           c.enableOffhandBlocker);
            toml.set("enableInventoryWarning",         c.enableInventoryWarning);
            // ── Commands ─────────────────────────────────────────────────────────────────
            toml.setComment("enableCommandShortforms",
                " ── Commands (Kurzformen: 1k → 1000, 1.5m → 1500000 …) ─────────────────────");
            toml.set("enableCommandShortforms",        c.enableCommandShortforms);
            // ── Netzwerk: Refresh-Intervalle ──────────────────────────────────────────────
            toml.setComment("marketRefreshIntervalSeconds",
                " ── Netzwerk: Refresh-Intervalle (Sekunden) ─────────────────────────────────");
            toml.set("marketRefreshIntervalSeconds",   c.marketRefreshIntervalSeconds);
            toml.set("merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds);
            // ── UI-Overlays ───────────────────────────────────────────────────────────────
            toml.setComment("showContainerOverlay",
                " ── UI-Overlays ──────────────────────────────────────────────────────────────");
            toml.set("showContainerOverlay",           c.showContainerOverlay);
            toml.set("showQuickButtons",               c.showQuickButtons);
            toml.set("shulkerRecursion",               c.shulkerRecursion);
            // ── Amboss-Normalisierung ─────────────────────────────────────────────────────
            toml.setComment("enableAnvilNormalization",
                " ── Amboss-Normalisierung (Kurzformen im Umbenennen-Feld expandieren) ────────");
            toml.set("enableAnvilNormalization",       c.enableAnvilNormalization);
            // ── Discord Rich Presence ─────────────────────────────────────────────────────
            toml.setComment("enableDiscordRpc",
                " ── Discord Rich Presence (Standard: deaktiviert) ────────────────────────────");
            toml.set("enableDiscordRpc",               c.enableDiscordRpc);
            // ── Netzwerk: Proxy/API/UA ────────────────────────────────────────────────────
            toml.setComment("proxyHost",
                " ── Netzwerk: Proxy (leer / 0 = Direktverbindung) ───────────────────────────");
            toml.set("proxyHost",                      c.proxyHost);
            toml.set("proxyPort",                      c.proxyPort);
            toml.setComment("apiKey",
                " ── Netzwerk: API-Key (non-leer → Header \"Authorization: Bearer <key>\") ──────");
            toml.set("apiKey",                         c.apiKey);
            toml.setComment("customUserAgent",
                " ── Netzwerk: User-Agent (leer = auto \"Visotaris/<ver> (MC/<ver>)\") ────");
            toml.set("customUserAgent",                c.customUserAgent);
            toml.save();
        } catch (Exception e) {
            VisotarisLogger.error("Konfiguration konnte nicht gespeichert werden: {}", e.getMessage());
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
}
