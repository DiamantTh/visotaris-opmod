package systems.diath.visotaris_opmod.config;

/**
 * Alle konfigurierbaren Einstellungen der Visotaris Mod.
 * Defaults sind hier als Feldinitialisierungen definiert –
 * keine doppelte Default-Quelle nötig.
 *
 * Serialisierung/Deserialisierung erfolgt explizit im {@link ConfigManager}
 * über night-config TOML – kein Reflection, kein Gson.
 */
public final class VisotarisConfig {

    // ── Modus ─────────────────────────────────────────────────────────────────
    /**
     * Observer-Modus: deaktiviert sämtliche Ingame-Eingriffe (Tooltips, HUD,
     * Container-Overlay, Quick-Buttons, Schutzlogik, Offhand-Blocker, Job-Tracker,
     * Command-Kurzformen, Amboss-Normalisierung, Discord RPC).
     *
     * Aktiv bleiben ausschließlich:
     *   - Hintergrund-Datenabruf (Markt + Merchant/Shard)
     *   - Caches + Persistenz
     *   - Web-UI (sofern separat aktiviert)
     *   - Settings-/Refresh-Keybinds
     */
    public boolean observerModeOnly = false;

    // ── Anzeige ───────────────────────────────────────────────────────────────
    public boolean showMarketTooltips   = true;
    public boolean showHud              = true;
    public boolean showContainerOverlay = true;
    public boolean showQuickButtons     = true;
    public boolean shulkerRecursion     = true;

    // ── Schutz ────────────────────────────────────────────────────────────────
    public boolean enableRenameProtection = true;
    public boolean enableSignProtection   = true;
    public boolean enableOffhandBlocker   = true;
    public boolean enableInventoryWarning = true;

    // ── Features ──────────────────────────────────────────────────────────────
    public boolean enableJobTracker        = true;
    public boolean enableCommandShortforms = true;   // Kurzformen: 1k → 1000, 1.5m → 1500000 …
    public boolean enableAnvilNormalization = true;  // Kurzformen im Amboss-Umbenennenfeld expandieren
    public boolean enableDiscordRpc        = false;  // Standard: deaktiviert
    public String  discordApplicationId    = "";     // Discord Developer Portal → Application ID
    public boolean enableDiscordScreenshots = false; // Standard: deaktiviert
    public boolean saveDiscordScreenshotsLocally = true;
    public String  discordScreenshotMessage = "Visotaris Screenshot";
    public final DiscordScreenshotTarget[] discordScreenshotTargets = {
        new DiscordScreenshotTarget("Ziel 1"),
        new DiscordScreenshotTarget("Ziel 2"),
        new DiscordScreenshotTarget("Ziel 3"),
        new DiscordScreenshotTarget("Ziel 4"),
        new DiscordScreenshotTarget("Ziel 5")
    };

    // ── Netzwerk ──────────────────────────────────────────────────────────────
    public int     marketRefreshIntervalSeconds   = 300;
    public int     merchantRefreshIntervalSeconds = 300;
    public boolean enableWebUi    = false;          // lokaler HTTP-Server (localhost:webUiPort)
    public int     webUiPort      = 7780;
    public String  proxyHost      = "";             // leer = Direktverbindung
    public int     proxyPort      = 0;
    public String  apiKey         = "";             // non-leer → Header "Authorization: Bearer <key>"
    public String  customUserAgent = "";            // leer = auto "Visotaris-OPMod/<ver> (…)"

    /**
     * Convenience: liefert {@code true}, wenn Ingame-Eingriffe (Tooltips, HUD,
     * Mixins, Schutzlogik etc.) erlaubt sind. Im Observer-Modus deaktiviert.
     */
    public boolean ingameFeaturesEnabled() {
        return !observerModeOnly;
    }

    public boolean isDiscordScreenshotTargetEnabled(int index) {
        return isTargetIndex(index) && discordScreenshotTargets[index].enabled;
    }

    public String getDiscordScreenshotTargetName(int index) {
        return isTargetIndex(index) ? discordScreenshotTargets[index].name : "";
    }

    public String getDiscordScreenshotWebhookUrl(int index) {
        return isTargetIndex(index) ? discordScreenshotTargets[index].webhookUrl : "";
    }

    private static boolean isTargetIndex(int index) {
        return index >= 0 && index < 5;
    }

    public static final class DiscordScreenshotTarget {
        public boolean enabled = false;
        public String name;
        public String webhookUrl = "";

        public DiscordScreenshotTarget(String name) {
            this.name = name;
        }
    }
}
