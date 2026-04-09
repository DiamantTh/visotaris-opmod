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

    // ── Netzwerk ──────────────────────────────────────────────────────────────
    public int     marketRefreshIntervalSeconds   = 300;
    public int     merchantRefreshIntervalSeconds = 300;
    public boolean enableWebUi    = false;          // lokaler HTTP-Server (localhost:webUiPort)
    public int     webUiPort      = 7780;
    public String  proxyHost      = "";             // leer = Direktverbindung
    public int     proxyPort      = 0;
    public String  apiKey         = "";             // non-leer → Header "Authorization: Bearer <key>"
    public String  customUserAgent = "";            // leer = auto "Visotaris-OPMod/<ver> (…)"
}
