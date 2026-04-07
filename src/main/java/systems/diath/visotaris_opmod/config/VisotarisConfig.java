package systems.diath.visotaris_opmod.config;

/**
 * Alle konfigurierbaren Einstellungen der Visotaris OPMod.
 * Defaults sind hier als Feldinitialisierungen definiert –
 * keine doppelte Default-Quelle nötig.
 *
 * Serialisierung/Deserialisierung erfolgt explizit im {@link ConfigManager}
 * über night-config TOML – kein Reflection, kein Gson.
 */
public final class VisotarisConfig {

    // ── Allgemein ──────────────────────────────────────────────────────────────
    public boolean showMarketTooltips       = true;
    public boolean showHud                  = true;

    // ── Job-Tracker ───────────────────────────────────────────────────────────
    public boolean enableJobTracker         = true;

    // ── Schutz ────────────────────────────────────────────────────────────────
    public boolean enableRenameProtection   = true;
    public boolean enableSignProtection     = true;
    public boolean enableOffhandBlocker     = true;
    public boolean enableInventoryWarning   = true;

    // ── Commands ──────────────────────────────────────────────────────────────
    // Kurzformen: 1k → 1000, 1.5m → 1500000 usw. in /pay, bank einzahlen, …
    public boolean enableCommandShortforms  = true;

    // ── Netzwerk ──────────────────────────────────────────────────────────────
    public int marketRefreshIntervalSeconds   = 60;
    public int merchantRefreshIntervalSeconds = 60;

    // ── UI-Overlays ───────────────────────────────────────────────────────────
    public boolean showContainerOverlay = true;
    public boolean showQuickButtons     = true;
    public boolean shulkerRecursion     = true;

    // ── Amboss-Normalisierung ─────────────────────────────────────────────────
    // Kurzformen (1k, 2.5m, …) im Amboss-Umbenennenfeld zu vollen Zahlen expandieren
    public boolean enableAnvilNormalization = true;

    // ── Discord Rich Presence ─────────────────────────────────────────────────
    // Standard: deaktiviert (andere RPC-Mods haben Vorrang)
    public boolean enableDiscordRpc = false;

    // ── Netzwerk: Proxy ───────────────────────────────────────────────────────
    // Leer/0 = Direktverbindung; z.B. proxyHost="127.0.0.1", proxyPort=8080
    public String proxyHost = "";
    public int    proxyPort = 0;

    // ── Netzwerk: API-Key ─────────────────────────────────────────────────────
    // Wenn non-leer: Header "Authorization: Bearer <apiKey>" wird mitgesendet
    public String apiKey = "";

    // ── Netzwerk: User-Agent ──────────────────────────────────────────────────
    // Leer = dynamisch ("Visotaris-OPMod/<ver> (MC/<ver>; Fabric/<ver>; ...)")
    public String customUserAgent = "";
}
