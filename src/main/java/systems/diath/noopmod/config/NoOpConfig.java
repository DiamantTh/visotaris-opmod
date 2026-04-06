package systems.diath.noopmod.config;

/**
 * Alle konfigurierbaren Einstellungen der No OP.Mod.
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

    // ── Netzwerk ──────────────────────────────────────────────────────────────
    public int marketRefreshIntervalSeconds   = 60;
    public int merchantRefreshIntervalSeconds = 60;

    // ── UI-Overlays ───────────────────────────────────────────────────────────
    public boolean showContainerOverlay = true;
    public boolean showQuickButtons     = true;

    // ── Discord Rich Presence ─────────────────────────────────────────────────
    // Standard: deaktiviert (andere RPC-Mods haben Vorrang)
    public boolean enableDiscordRpc = false;
}
