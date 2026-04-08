package systems.diath.visotaris_opmod.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris_opmod.VisotarisLogger;

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

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            VisotarisLogger.info("Neue Konfiguration erstellt: {}", configPath);
            return;
        }
        try (CommentedFileConfig toml = CommentedFileConfig.builder(configPath, TomlFormat.instance()).build()) {
            toml.load();
            VisotarisConfig c = new VisotarisConfig();
            // ── Anzeige (neu: anzeige.x; alt: x) ─────────────────────────────────────
            c.showMarketTooltips   = toml.getOrElse("anzeige.showMarketTooltips",   toml.getOrElse("showMarketTooltips",   c.showMarketTooltips));
            c.showHud              = toml.getOrElse("anzeige.showHud",              toml.getOrElse("showHud",              c.showHud));
            c.showContainerOverlay = toml.getOrElse("anzeige.showContainerOverlay", toml.getOrElse("showContainerOverlay", c.showContainerOverlay));
            c.showQuickButtons     = toml.getOrElse("anzeige.showQuickButtons",     toml.getOrElse("showQuickButtons",     c.showQuickButtons));
            c.shulkerRecursion     = toml.getOrElse("anzeige.shulkerRecursion",     toml.getOrElse("shulkerRecursion",     c.shulkerRecursion));
            // ── Schutz ────────────────────────────────────────────────────────────────
            c.enableRenameProtection = toml.getOrElse("schutz.enableRenameProtection", toml.getOrElse("enableRenameProtection", c.enableRenameProtection));
            c.enableSignProtection   = toml.getOrElse("schutz.enableSignProtection",   toml.getOrElse("enableSignProtection",   c.enableSignProtection));
            c.enableOffhandBlocker   = toml.getOrElse("schutz.enableOffhandBlocker",   toml.getOrElse("enableOffhandBlocker",   c.enableOffhandBlocker));
            c.enableInventoryWarning = toml.getOrElse("schutz.enableInventoryWarning", toml.getOrElse("enableInventoryWarning", c.enableInventoryWarning));
            // ── Features ──────────────────────────────────────────────────────────────
            c.enableJobTracker         = toml.getOrElse("features.enableJobTracker",         toml.getOrElse("enableJobTracker",         c.enableJobTracker));
            c.enableCommandShortforms  = toml.getOrElse("features.enableCommandShortforms",  toml.getOrElse("enableCommandShortforms",  c.enableCommandShortforms));
            c.enableAnvilNormalization = toml.getOrElse("features.enableAnvilNormalization", toml.getOrElse("enableAnvilNormalization", c.enableAnvilNormalization));
            c.enableDiscordRpc         = toml.getOrElse("features.enableDiscordRpc",         toml.getOrElse("enableDiscordRpc",         c.enableDiscordRpc));
            // ── Netzwerk ──────────────────────────────────────────────────────────────
            c.marketRefreshIntervalSeconds   = getInt(toml, "netzwerk.marketRefreshIntervalSeconds",   getInt(toml, "marketRefreshIntervalSeconds",   c.marketRefreshIntervalSeconds));
            c.merchantRefreshIntervalSeconds = getInt(toml, "netzwerk.merchantRefreshIntervalSeconds", getInt(toml, "merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds));
            c.enableWebUi    = toml.getOrElse("netzwerk.enableWebUi",    toml.getOrElse("enableWebUi",    c.enableWebUi));
            c.webUiPort      = getInt(toml, "netzwerk.webUiPort",        getInt(toml, "webUiPort",        c.webUiPort));
            c.proxyHost      = toml.getOrElse("netzwerk.proxyHost",      toml.getOrElse("proxyHost",      c.proxyHost));
            c.proxyPort      = getInt(toml, "netzwerk.proxyPort",        getInt(toml, "proxyPort",        c.proxyPort));
            c.apiKey         = toml.getOrElse("netzwerk.apiKey",         toml.getOrElse("apiKey",         c.apiKey));
            c.customUserAgent = toml.getOrElse("netzwerk.customUserAgent", toml.getOrElse("customUserAgent", c.customUserAgent));
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
            // ── [anzeige] ─────────────────────────────────────────────────────────────
            toml.setComment("anzeige", " HUD, Tooltips und UI-Overlays");
            toml.set("anzeige.showMarketTooltips",   c.showMarketTooltips);
            toml.set("anzeige.showHud",              c.showHud);
            toml.set("anzeige.showContainerOverlay", c.showContainerOverlay);
            toml.set("anzeige.showQuickButtons",     c.showQuickButtons);
            toml.set("anzeige.shulkerRecursion",     c.shulkerRecursion);
            // ── [schutz] ──────────────────────────────────────────────────────────────
            toml.setComment("schutz", " Schutzmechanismen (Rename, Sign, Offhand, Inventar)");
            toml.set("schutz.enableRenameProtection", c.enableRenameProtection);
            toml.set("schutz.enableSignProtection",   c.enableSignProtection);
            toml.set("schutz.enableOffhandBlocker",   c.enableOffhandBlocker);
            toml.set("schutz.enableInventoryWarning", c.enableInventoryWarning);
            // ── [features] ────────────────────────────────────────────────────────────
            toml.setComment("features", " Spielmechanik-Features");
            toml.set("features.enableJobTracker",         c.enableJobTracker);
            toml.set("features.enableCommandShortforms",  c.enableCommandShortforms);
            toml.set("features.enableAnvilNormalization", c.enableAnvilNormalization);
            toml.set("features.enableDiscordRpc",         c.enableDiscordRpc);
            // ── [netzwerk] ────────────────────────────────────────────────────────────
            toml.setComment("netzwerk", " Refresh-Intervalle (Sekunden), Web-UI & Proxy");
            toml.set("netzwerk.marketRefreshIntervalSeconds",   c.marketRefreshIntervalSeconds);
            toml.set("netzwerk.merchantRefreshIntervalSeconds", c.merchantRefreshIntervalSeconds);
            toml.set("netzwerk.enableWebUi",    c.enableWebUi);
            toml.set("netzwerk.webUiPort",      c.webUiPort);
            toml.set("netzwerk.proxyHost",      c.proxyHost);
            toml.set("netzwerk.proxyPort",      c.proxyPort);
            toml.set("netzwerk.apiKey",         c.apiKey);
            toml.set("netzwerk.customUserAgent", c.customUserAgent);
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
