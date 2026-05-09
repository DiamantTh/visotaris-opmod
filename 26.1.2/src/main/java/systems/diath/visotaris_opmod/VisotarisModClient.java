package systems.diath.visotaris_opmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.api.MarketHistoryApiClient;
import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.cache.PriceHistoryCache;
import systems.diath.visotaris_opmod.cache.ShardCache;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.services.JobTrackerService;
import systems.diath.visotaris_opmod.services.MarketSyncService;
import systems.diath.visotaris_opmod.services.MerchantSyncService;
import systems.diath.visotaris_opmod.services.CommandRewriteService;
import systems.diath.visotaris_opmod.services.DiscordPresenceService;
import systems.diath.visotaris_opmod.services.DiscordScreenshotService;
import systems.diath.visotaris_opmod.services.KeybindService;
import systems.diath.visotaris_opmod.services.MinecraftScreenshotCaptureBackend;
import systems.diath.visotaris_opmod.services.PendingConfirmationService;
import systems.diath.visotaris_opmod.services.TooltipValueService;
import systems.diath.visotaris_opmod.web.WebServer;

@Environment(EnvType.CLIENT)
public class VisotarisModClient implements ClientModInitializer {

    public static final String MOD_ID   = VisotarisConst.MOD_ID;
    public static final String MOD_NAME = VisotarisConst.MOD_NAME;

    private static VisotarisModClient instance;

    // Caches (gemeinsam genutzte In-Memory-Daten)
    private final MarketCache marketCache = new MarketCache();
    private final ShardCache  shardCache  = new ShardCache();

    // Services (Businesslogik, von Caches getrennt)
    private ConfigManager              configManager;
    private MarketSyncService          marketSyncService;
    private MerchantSyncService        merchantSyncService;
    private JobTrackerService          jobTrackerService;
    private TooltipValueService        tooltipValueService;
    private PendingConfirmationService pendingConfirmationService;
    private DiscordPresenceService      discordPresenceService;
    private DiscordScreenshotService    discordScreenshotService;
    private CommandRewriteService       commandRewriteService;
    private KeybindService              keybindService;

    // Web-UI
    private WebServer         webServer;
    private PriceHistoryCache priceHistoryCache;

    @Override
    public void onInitializeClient() {
        instance = this;

        // 1. Config laden
        configManager = new ConfigManager();
        configManager.load();

        // 2. Services aufbauen (Abhängigkeiten explizit injizieren)
        marketSyncService         = new MarketSyncService(marketCache, configManager);
        merchantSyncService       = new MerchantSyncService(shardCache, configManager);
        tooltipValueService       = new TooltipValueService(marketCache, shardCache, configManager);
        jobTrackerService         = new JobTrackerService(configManager);
        pendingConfirmationService = new PendingConfirmationService(configManager);
        discordPresenceService     = new DiscordPresenceService(
            configManager,
            jobTrackerService,
            DiscordPresenceService.PresenceMode.ADVANCED
        );
        discordScreenshotService    = new DiscordScreenshotService(configManager, new MinecraftScreenshotCaptureBackend());
        commandRewriteService      = new CommandRewriteService(configManager);
        keybindService             = new KeybindService(configManager, marketSyncService, merchantSyncService, discordScreenshotService);
        // 3. Hintergrundfetcher starten
        marketSyncService.start();
        merchantSyncService.start();

        // 4. Fabric-Events registrieren (Chat → Jobtracker)
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
            jobTrackerService.processMessage(message.getString())
        );

        // 4a. Command-Kurzformen expandieren (läuft VOR ALLOW_COMMAND)
        ClientSendMessageEvents.MODIFY_COMMAND.register(cmd -> {
            String result = commandRewriteService.rewrite(cmd);
            return result != null ? result : cmd;
        });

        // 4b. /rename und /sign abfangen
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            PendingConfirmationService.Intercepted iv = pendingConfirmationService.tryIntercept(command);
            if (iv == null) return true;
            String label = iv.type() == systems.diath.visotaris_opmod.model.PendingAction.Type.RENAME ? "Rename" : "Sign";
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.sendSystemMessage(Component.empty()
                .append(Component.literal("§e[Visotaris] §7" + label + ": \"§f" + iv.text() + "§7\"  "))
                .append(Component.literal("§a§l[✓ Bestätigen]").withStyle(s ->
                    s.withClickEvent(new ClickEvent.RunCommand(iv.confirmCmd()))))
                .append(Component.literal("  "))
                .append(Component.literal("§c§l[✗ Abbrechen]").withStyle(s ->
                    s.withClickEvent(new ClickEvent.RunCommand(iv.cancelCmd())))));
            return false;
        });

        // 5. Tooltip-Event (Fabric API)
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register(
            (stack, context, type, lines) -> {
                if (configManager.getConfig().showMarketTooltips) {
                    tooltipValueService.appendTooltips(stack, lines);
                }
            }
        );

        // 6. Discord Presence (nur Events registrieren; connect passiert erst bei JOIN)
        discordPresenceService.registerEvents();

        // 7. Keybinds registrieren
        keybindService.registerTick();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> discordScreenshotService.shutdown());

        // 8. Offhand-Blocker: Tastendrücke für F-Taste vor handleInputEvents() schlucken
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            var cfg = configManager.getConfig();
            if (!cfg.ingameFeaturesEnabled()) return;
            if (!cfg.enableOffhandBlocker) return;
            if (client.options == null) return;
            //noinspection StatementWithEmptyBody
            while (client.options.keySwapOffhand.consumeClick()) { /* blockiert */ }
        });

        VisotarisLogger.info("{} v{} initialisiert (MC 26.1.2).", MOD_NAME, VisotarisModClient.class.getPackage().getImplementationVersion());

        // 9. Web-UI starten (Standard: deaktiviert – via Config aktivierbar)
        MarketHistoryApiClient historyApiClient = new MarketHistoryApiClient(configManager);
        priceHistoryCache = new PriceHistoryCache(historyApiClient);
        webServer = new WebServer(
            configManager.getConfig().webUiPort,
            marketCache, shardCache, priceHistoryCache
        );
        if (configManager.getConfig().enableWebUi) {
            webServer.start();
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> webServer.stop());
    }

    // --- Globaler Zugriffspunkt ---

    public static VisotarisModClient getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager()                         { return configManager; }
    public MarketSyncService getMarketSyncService()                 { return marketSyncService; }
    public MerchantSyncService getMerchantSyncService()             { return merchantSyncService; }
    public JobTrackerService getJobTrackerService()                 { return jobTrackerService; }
    public TooltipValueService getTooltipValueService()             { return tooltipValueService; }
    public PendingConfirmationService getPendingConfirmationService(){ return pendingConfirmationService; }
    public MarketCache getMarketCache()                             { return marketCache; }
    public ShardCache getShardCache()                               { return shardCache; }
}
