package systems.diath.visotaris;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import systems.diath.visotaris.VisotarisConst;
import systems.diath.visotaris.cache.MarketCache;
import systems.diath.visotaris.cache.ShardCache;
import systems.diath.visotaris.commands.VisotarisCommands;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.services.InventoryValuationService;
import systems.diath.visotaris.services.JobTrackerService;
import systems.diath.visotaris.services.MarketSyncService;
import systems.diath.visotaris.services.MerchantSyncService;
import systems.diath.visotaris.services.CommandRewriteService;
import systems.diath.visotaris.services.DiscordPresenceService;
import systems.diath.visotaris.services.KeybindService;
import systems.diath.visotaris.services.PendingConfirmationService;
import systems.diath.visotaris.services.TooltipValueService;
import systems.diath.visotaris.ui.HudOverlay;

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
    private InventoryValuationService  inventoryValuationService;
    private PendingConfirmationService pendingConfirmationService;
    private DiscordPresenceService      discordPresenceService;
    private CommandRewriteService       commandRewriteService;
    private KeybindService              keybindService;

    // UI
    private HudOverlay hudOverlay;

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
        inventoryValuationService = new InventoryValuationService(marketCache, shardCache, configManager);
        jobTrackerService         = new JobTrackerService(configManager);
        pendingConfirmationService = new PendingConfirmationService(configManager);
        discordPresenceService     = new DiscordPresenceService(configManager);
        commandRewriteService      = new CommandRewriteService(configManager);
        keybindService             = new KeybindService(configManager, marketSyncService, merchantSyncService);
        // 3. Hintergrundfetcher starten
        marketSyncService.start();
        merchantSyncService.start();

        // 4. Fabric-Events registrieren (Chat → Jobtracker)
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
            jobTrackerService.processMessage(message.getString())
        );

        // 4a. Command-Kurzformen expandieren (läuft VOR ALLOW_COMMAND)
        ClientSendMessageEvents.MODIFY_COMMAND.register(cmd ->
            commandRewriteService.rewrite(cmd)
        );

        // 4b. /rename und /sign abfangen
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            PendingConfirmationService.Intercepted iv = pendingConfirmationService.tryIntercept(command);
            if (iv == null) return true;
            String label = iv.type() == systems.diath.visotaris.model.PendingAction.Type.RENAME ? "Rename" : "Sign";
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.sendMessage(Text.empty()
                .append(Text.literal("§e[Visotaris] §7" + label + ": \"§f" + iv.text() + "§7\"  "))
                .append(Text.literal("§a§l[✓ Bestätigen]").styled(s ->
                    s.withClickEvent(new ClickEvent.RunCommand(iv.confirmCmd()))))
                .append(Text.literal("  "))
                .append(Text.literal("§c§l[✗ Abbrechen]").styled(s ->
                    s.withClickEvent(new ClickEvent.RunCommand(iv.cancelCmd())))), false);
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

        // 6. HUD registrieren
        // TODO: Auf HudElementRegistry migrieren, sobald die neue API stabil ist (fabric-rendering-v1 ≥ 16.x).
        hudOverlay = new HudOverlay(jobTrackerService, inventoryValuationService, configManager);
        @SuppressWarnings("deprecation")
        var hudEvent = HudRenderCallback.EVENT;
        hudEvent.register(hudOverlay::render);

        // 7. Client-Commands registrieren
        VisotarisCommands.register(pendingConfirmationService);

        // 8. Discord Presence (nur Events registrieren; connect passiert erst bei JOIN)
        discordPresenceService.registerEvents();

        // 9. Keybinds registrieren
        keybindService.registerTick();

        // 10. Offhand-Blocker: Tastendrücke für F-Taste vor handleInputEvents() schlucken
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!configManager.getConfig().enableOffhandBlocker) return;
            if (client.options == null) return;
            //noinspection StatementWithEmptyBody
            while (client.options.swapHandsKey.wasPressed()) { /* blockiert */ }
        });

        VisotarisLogger.info("{} v{} initialisiert (MC 1.21.11).", MOD_NAME, VisotarisModClient.class.getPackage().getImplementationVersion());
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
    public InventoryValuationService getInventoryValuationService() { return inventoryValuationService; }
    public PendingConfirmationService getPendingConfirmationService(){ return pendingConfirmationService; }
    public MarketCache getMarketCache()                             { return marketCache; }
    public ShardCache getShardCache()                               { return shardCache; }
}
