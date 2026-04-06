package systems.diath.noopmod.config;

/**
 * Alle konfigurierbaren Einstellungen der Visotaris OPMod.
 * Dieses POJO wird direkt von Gson serialisiert/deserialisiert.
 *
 * Defaults sind hier als Feldinitialisierungen definiert –
 * keine doppelte Default-Quelle (kein separates JSON nötig).
 */
public final class NoOpConfig {

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

    // ── Netzwerk: User-Agent ──────────────────────────────────────────────────
    // Leer = dynamisch ("Visotaris-OPMod/<ver> (MC/<ver>; Fabric/<ver>; ...)")
    public String customUserAgent = "";
}
