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

    public VisotarisConfig() { }

    /** Deep copy used by the in-game editor until the user confirms Save. */
    public VisotarisConfig(VisotarisConfig source) {
        observerModeOnly = source.observerModeOnly;
        showMarketTooltips = source.showMarketTooltips;
        showHud = source.showHud;
        showContainerOverlay = source.showContainerOverlay;
        showQuickButtons = source.showQuickButtons;
        shulkerRecursion = source.shulkerRecursion;
        tooltipShowBuyPrice = source.tooltipShowBuyPrice;
        tooltipShowSellPrice = source.tooltipShowSellPrice;
        tooltipShowMerchantRates = source.tooltipShowMerchantRates;
        tooltipShowShardRates = source.tooltipShowShardRates;
        tooltipShowDataAge = source.tooltipShowDataAge;
        tooltipShowStaleData = source.tooltipShowStaleData;
        tooltipMaxAgeSeconds = source.tooltipMaxAgeSeconds;
        priceAlertsEnabled = source.priceAlertsEnabled;
        auctionLiveUpdatesEnabled = source.auctionLiveUpdatesEnabled;
        priceAlertRules = new java.util.ArrayList<>();
        for (PriceAlertRule rule : source.priceAlertRules) priceAlertRules.add(PriceAlertRule.fromMap(rule.toMap()));
        enableRenameProtection = source.enableRenameProtection;
        enableSignProtection = source.enableSignProtection;
        enableOffhandBlocker = source.enableOffhandBlocker;
        enableInventoryWarning = source.enableInventoryWarning;
        enableJobTracker = source.enableJobTracker;
        enableCommandShortforms = source.enableCommandShortforms;
        enableAnvilNormalization = source.enableAnvilNormalization;
        enableDiscordRpc = source.enableDiscordRpc;
        discordApplicationId = source.discordApplicationId;
        saveDiscordScreenshotsLocally = source.saveDiscordScreenshotsLocally;
        verboseDiscordScreenshotLogging = source.verboseDiscordScreenshotLogging;
        for (int i = 0; i < discordScreenshotTargets.length; i++) {
            discordScreenshotTargets[i].enabled = source.discordScreenshotTargets[i].enabled;
            discordScreenshotTargets[i].name = source.discordScreenshotTargets[i].name;
            discordScreenshotTargets[i].webhookUrl = source.discordScreenshotTargets[i].webhookUrl;
        }
        marketRefreshIntervalSeconds = source.marketRefreshIntervalSeconds;
        merchantRefreshIntervalSeconds = source.merchantRefreshIntervalSeconds;
        enableWebUi = source.enableWebUi;
        webUiPort = source.webUiPort;
        proxyType = source.proxyType;
        proxyHost = source.proxyHost;
        proxyPort = source.proxyPort;
        customUserAgent = source.customUserAgent;
        systemPasswordHash = source.systemPasswordHash;
    }

    // ── Modus ─────────────────────────────────────────────────────────────────
    /**
     * Observer-Modus: deaktiviert sämtliche Ingame-Eingriffe (Tooltips, HUD,
     * Container-Overlay, Quick-Buttons, Schutzlogik, Offhand-Blocker, Job-Tracker,
     * Command-Kurzformen, Amboss-Normalisierung).
     *
     * Aktiv bleiben ausschließlich:
     *   - Hintergrund-Datenabruf (Markt + Merchant/Shard)
     *   - Caches + Persistenz
     *   - Web-Interface (sofern separat aktiviert)
     *   - Discord RPC (sofern separat aktiviert)
     *   - Settings-/Refresh-Keybinds
     */
    public boolean observerModeOnly = false;

    // ── Anzeige ───────────────────────────────────────────────────────────────
    public boolean showMarketTooltips   = true;
    public boolean showHud              = true;
    public boolean showContainerOverlay = true;
    public boolean showQuickButtons     = true;
    public boolean shulkerRecursion     = true;

    // ── Client-side tooltip details ──────────────────────────────────────────
    public boolean tooltipShowBuyPrice = true;
    public boolean tooltipShowSellPrice = true;
    public boolean tooltipShowMerchantRates = true;
    public boolean tooltipShowShardRates = true;
    public boolean tooltipShowDataAge = false;
    public boolean tooltipShowStaleData = true;
    public int tooltipMaxAgeSeconds = 900;

    // ── Client-side price alerts ─────────────────────────────────────────────
    public boolean priceAlertsEnabled = true;
    public java.util.List<PriceAlertRule> priceAlertRules = new java.util.ArrayList<>();

    // ── Auction house ────────────────────────────────────────────────────────
    /** Optional live updates; manual snapshot refresh remains available when disabled. */
    public boolean auctionLiveUpdatesEnabled = false;

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
    public boolean saveDiscordScreenshotsLocally = true;
    public boolean verboseDiscordScreenshotLogging = false;
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
    public String  proxyType      = "HTTP";         // HTTP, HTTPS(TLS zum Proxy) oder SOCKS
    public String  proxyHost      = "";             // leer = Direktverbindung; Proxy für ausgehende API-Requests
    public int     proxyPort      = 0;
    public String  customUserAgent = "";            // leer = auto "Visotaris-OPMod/<ver> (…)"

    // ── Lokaler Systemzugang ─────────────────────────────────────────────────
    /** Ausschließlich ein Argon2id-Hash, niemals ein Klartextpasswort. */
    public String systemPasswordHash = "";

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
