package systems.diath.noopmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import systems.diath.noopmod.NoOpConst;
import systems.diath.noopmod.cache.MarketCache;
import systems.diath.noopmod.cache.ShardCache;
import systems.diath.noopmod.commands.NoOpCommands;
import systems.diath.noopmod.config.ConfigManager;
import systems.diath.noopmod.services.InventoryValuationService;
import systems.diath.noopmod.services.JobTrackerService;
import systems.diath.noopmod.services.MarketSyncService;
import systems.diath.noopmod.services.MerchantSyncService;
import systems.diath.noopmod.services.CommandRewriteService;
import systems.diath.noopmod.services.DiscordPresenceService;
import systems.diath.noopmod.services.KeybindService;
import systems.diath.noopmod.services.PendingConfirmationService;
import systems.diath.noopmod.services.TooltipValueService;
import systems.diath.noopmod.ui.HudOverlay;

@Environment(EnvType.CLIENT)
public class NoOpModClient implements ClientModInitializer {

    public static final String MOD_ID   = NoOpConst.MOD_ID;
    public static final String MOD_NAME = NoOpConst.MOD_NAME;

    private static NoOpModClient instance;

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
        marketSyncService          = new MarketSyncService(marketCache, configManager);
        merchantSyncService        = new MerchantSyncService(shardCache, configManager);
        tooltipValueService        = new TooltipValueService(marketCache, shardCache, configManager);
        inventoryValuationService  = new InventoryValuationService(marketCache, shardCache, configManager);
        jobTrackerService          = new JobTrackerService(configManager);
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

        // 4b. /rename und /sign vor dem Absenden abfangen (1.21.4: alte ClickEvent-API)
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            PendingConfirmationService.Intercepted iv = pendingConfirmationService.tryIntercept(command);
            if (iv == null) return true;
            String label = iv.type() == systems.diath.noopmod.model.PendingAction.Type.RENAME ? "Rename" : "Sign";
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.sendMessage(Text.empty()
                .append(Text.literal("§e[Visotaris OPMod] §7" + label + ": \"§f" + iv.text() + "§7\"  "))
                .append(Text.literal("§a§l[✓ Bestätigen]").styled(s ->
                    s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, iv.confirmCmd()))))
                .append(Text.literal("  "))
                .append(Text.literal("§c§l[✗ Abbrechen]").styled(s ->
                    s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, iv.cancelCmd())))), false);
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
        hudOverlay = new HudOverlay(jobTrackerService, inventoryValuationService, configManager);
        HudRenderCallback.EVENT.register(hudOverlay::render);

        // 7. Client-Commands registrieren
        NoOpCommands.register(pendingConfirmationService);

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

        NoOpLogger.info("{} v{} initialisiert (MC 1.21.4).", MOD_NAME,
            NoOpModClient.class.getPackage().getImplementationVersion());
    }

    // --- Globaler Zugriffspunkt ---

    public static NoOpModClient getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager()                          { return configManager; }
    public MarketSyncService getMarketSyncService()                  { return marketSyncService; }
    public MerchantSyncService getMerchantSyncService()              { return merchantSyncService; }
    public JobTrackerService getJobTrackerService()                  { return jobTrackerService; }
    public TooltipValueService getTooltipValueService()              { return tooltipValueService; }
    public InventoryValuationService getInventoryValuationService()  { return inventoryValuationService; }
    public PendingConfirmationService getPendingConfirmationService() { return pendingConfirmationService; }
    public MarketCache getMarketCache()                              { return marketCache; }
    public ShardCache getShardCache()                               { return shardCache; }
}
