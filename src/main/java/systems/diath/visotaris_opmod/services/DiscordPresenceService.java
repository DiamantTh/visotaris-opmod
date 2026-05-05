package systems.diath.visotaris_opmod.services;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.config.VisotarisConfig;
import systems.diath.visotaris_opmod.discord.ipc.DiscordIpcClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Discord Rich Presence – schmale Lifecycle-Integration.
 *
 * Standardmäßig deaktiviert (enableDiscordRpc = false), weil viele Spieler
 * bereits eine dedizierte RPC-Mod haben. Dieser Service belegt in dem Fall
 * keinerlei Discord-IPC-Ressourcen.
 */
public final class DiscordPresenceService {

    private final ConfigManager config;
    private final ExecutorService executor;
    private volatile boolean active = false;
    private volatile DiscordIpcClient client;
    private volatile long sessionStartEpochSeconds = 0;

    public DiscordPresenceService(ConfigManager config) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }

    /**
     * Lifecycle-Events registrieren. Wird einmalig beim Mod-Start aufgerufen.
     * Der eigentliche Connect passiert erst beim Server-Join.
     */
    public void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client)   -> onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> onClientStopping());
    }

    // ── Private Lifecycle ─────────────────────────────────────────────────────

    private void onJoin() {
        VisotarisConfig cfg = config.getConfig();
        if (!cfg.enableDiscordRpc) return;
        String appId = normalizeApplicationId(cfg.discordApplicationId);
        if (appId.isEmpty()) {
            VisotarisLogger.warn("DiscordPresence: aktiv, aber features.discordApplicationId ist leer");
            return;
        }
        sessionStartEpochSeconds = System.currentTimeMillis() / 1000L;
        executor.execute(() -> connectAndPublish(appId));
    }

    private void onDisconnect() {
        if (!active) return;
        active = false;
        executor.execute(this::clearAndClose);
    }

    private void onClientStopping() {
        active = false;
        executor.execute(this::clearAndClose);
        executor.shutdown();
    }

    private void connectAndPublish(String applicationId) {
        try {
            DiscordIpcClient ipc = client;
            if (ipc == null || !ipc.isOpen()) {
                ipc = new DiscordIpcClient(applicationId);
                ipc.connect();
                client = ipc;
                VisotarisLogger.debug("DiscordPresence: IPC verbunden");
            }
            ipc.setActivity(simpleActivityJson());
            active = true;
            VisotarisLogger.debug("DiscordPresence: Activity gesetzt");
        } catch (Exception e) {
            active = false;
            closeQuietly();
            VisotarisLogger.warn("DiscordPresence: IPC nicht verfügbar: {}", e.getMessage());
        }
    }

    private void clearAndClose() {
        try {
            DiscordIpcClient ipc = client;
            if (ipc != null && ipc.isOpen()) {
                ipc.clearActivity();
            }
        } catch (Exception e) {
            VisotarisLogger.debug("DiscordPresence: Activity konnte nicht geleert werden: {}", e.getMessage());
        } finally {
            closeQuietly();
            VisotarisLogger.debug("DiscordPresence: IPC geschlossen");
        }
    }

    private void closeQuietly() {
        DiscordIpcClient ipc = client;
        client = null;
        if (ipc != null) {
            try {
                ipc.close();
            } catch (Exception ignored) {
                // Cleanup darf den Client-Shutdown nicht stören.
            }
        }
    }

    private String simpleActivityJson() {
        String details = VisotarisConst.MOD_NAME;
        String state = "Spielt Minecraft";
        return "{"
            + "\"details\":" + DiscordIpcClient.quote(details) + ","
            + "\"state\":" + DiscordIpcClient.quote(state) + ","
            + "\"timestamps\":{\"start\":" + sessionStartEpochSeconds + "}"
            + "}";
    }

    private static String normalizeApplicationId(String value) {
        return value == null ? "" : value.strip();
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "Visotaris-DiscordRPC");
            thread.setDaemon(true);
            return thread;
        }
    }
}
