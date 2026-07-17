package systems.diath.visotaris_opmod.services;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.config.VisotarisConfig;
import systems.diath.visotaris_opmod.discord.ipc.DiscordIpcClient;
import systems.diath.visotaris_opmod.model.JobSnapshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.Locale;

/**
 * Discord Rich Presence – schmale Lifecycle-Integration.
 *
 * Standardmäßig deaktiviert (enableDiscordRpc = false), weil viele Spieler
 * bereits eine dedizierte RPC-Mod haben. Dieser Service belegt in dem Fall
 * keinerlei Discord-IPC-Ressourcen.
 */
public final class DiscordPresenceService {

    private static final long UPDATE_INTERVAL_MS = 15_000L;

    public enum PresenceMode {
        SIMPLE,
        ADVANCED
    }

    private final ConfigManager config;
    private final JobTrackerService jobTracker;
    private final PresenceMode mode;
    private final ExecutorService executor;
    private volatile boolean active = false;
    private volatile DiscordIpcClient client;
    private volatile boolean stopping = false;
    private volatile long sessionStartEpochSeconds = 0;
    private volatile long lastUpdateMs = 0;
    private volatile String lastActivityJson = "";

    public DiscordPresenceService(ConfigManager config) {
        this(config, null, PresenceMode.SIMPLE);
    }

    public DiscordPresenceService(ConfigManager config, JobTrackerService jobTracker, PresenceMode mode) {
        this.config = config;
        this.jobTracker = jobTracker;
        this.mode = mode;
        this.executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }

    /**
     * Lifecycle-Events registrieren. Wird einmalig beim Mod-Start aufgerufen.
     * Im Simple-Modus passiert der Connect erst beim Server-Join.
     * Im Advanced-Modus darf Presence auch Menü-/Disconnect-Zustände anzeigen.
     */
    public void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client)   -> onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> onClientStopping());
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    // ── Private Lifecycle ─────────────────────────────────────────────────────

    private void onJoin() {
        VisotarisConfig cfg = config.getConfig();
        if (!cfg.enableDiscordRpc) {
            deactivateIfNeeded();
            return;
        }
        String appId = resolveApplicationId(cfg);
        if (appId.isEmpty()) {
            deactivateIfNeeded();
            VisotarisLogger.warn("DiscordPresence: aktiv, aber features.discordApplicationId ist leer");
            return;
        }
        sessionStartEpochSeconds = System.currentTimeMillis() / 1000L;
        publishCurrentPresence(appId, Minecraft.getInstance(), true);
    }

    private void onDisconnect() {
        if (!active) return;
        active = false;
        sessionStartEpochSeconds = System.currentTimeMillis() / 1000L;
        submit(this::clearAndClose);
    }

    private void onClientStopping() {
        active = false;
        stopping = true;
        executor.execute(this::clearAndClose);
        executor.shutdown();
    }

    private void onTick(Minecraft minecraft) {
        VisotarisConfig cfg = config.getConfig();
        if (!cfg.enableDiscordRpc) {
            deactivateIfNeeded();
            return;
        }
        if (!isAdvanced() && !active) return;
        String appId = resolveApplicationId(cfg);
        if (appId.isEmpty()) {
            deactivateIfNeeded();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < UPDATE_INTERVAL_MS) return;
        lastUpdateMs = now;
        if (sessionStartEpochSeconds == 0) {
            sessionStartEpochSeconds = now / 1000L;
        }
        publishCurrentPresence(appId, minecraft, false);
    }

    private void publishCurrentPresence(String applicationId, Minecraft minecraft, boolean force) {
        String activityJson = isAdvanced()
            ? advancedActivityJson(minecraft)
            : simpleActivityJson();
        if (!force && activityJson.equals(lastActivityJson)) return;
        lastActivityJson = activityJson;
        submit(() -> connectAndPublish(applicationId, activityJson));
    }

    private void deactivateIfNeeded() {
        if (!active && client == null) return;
        active = false;
        submit(this::clearAndClose);
    }

    private void connectAndPublish(String applicationId, String activityJson) {
        try {
            DiscordIpcClient ipc = client;
            if (ipc == null || !ipc.isOpen()) {
                ipc = new DiscordIpcClient(applicationId);
                ipc.connect();
                client = ipc;
                VisotarisLogger.debug("DiscordPresence: IPC verbunden");
            }
            ipc.setActivity(activityJson);
            active = true;
            VisotarisLogger.debug("DiscordPresence: Activity gesetzt");
        } catch (Exception e) {
            active = false;
            lastActivityJson = "";
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
            lastActivityJson = "";
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

    private void submit(Runnable task) {
        if (stopping) return;
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // Shutdown gewinnt gegen nachlaufende Fabric-Events.
        }
    }

    private boolean isAdvanced() {
        return mode == PresenceMode.ADVANCED;
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

    private String advancedActivityJson(Minecraft minecraft) {
        PresenceText text = buildAdvancedText(minecraft);
        return "{"
            + "\"details\":" + DiscordIpcClient.quote(text.details()) + ","
            + "\"state\":" + DiscordIpcClient.quote(text.state()) + ","
            + "\"timestamps\":{\"start\":" + sessionStartEpochSeconds + "},"
            + "\"assets\":{"
            + "\"large_image\":\"visotaris\","
            + "\"large_text\":" + DiscordIpcClient.quote(VisotarisConst.MOD_NAME) + ","
            + "\"small_image\":" + DiscordIpcClient.quote(text.smallImage()) + ","
            + "\"small_text\":" + DiscordIpcClient.quote(text.smallText())
            + "}"
            + "}";
    }

    private PresenceText buildAdvancedText(Minecraft minecraft) {
        VisotarisConfig cfg = config.getConfig();
        String location = locationText(minecraft);
        JobSnapshot snapshot = jobTracker != null ? jobTracker.getSnapshot() : JobSnapshot.empty();
        if (cfg.enableJobTracker && snapshot.getJobName() != null && !snapshot.getJobName().isBlank()) {
            String jobName = capitalize(snapshot.getJobName());
            String details = "Job: " + jobName + " Lv. " + snapshot.getLevel();
            String state = formatRate(snapshot.getMoneyPerHour(), "$/h")
                + " · " + formatRate(snapshot.getXpPerHour(), "XP/h");
            return new PresenceText(details, state, "job", location);
        }

        return new PresenceText(VisotarisConst.MOD_NAME, location, "online", "Online");
    }

    private static String locationText(Minecraft minecraft) {
        if (minecraft == null) return "Im Client";
        var server = minecraft.getCurrentServer();
        if (server != null) {
            String label = firstNonBlank(server.name, server.ip, "Multiplayer");
            if (containsIgnoreCase(server.ip, "opsucht") || containsIgnoreCase(server.name, "opsucht")) {
                return "Auf OPSUCHT";
            }
            return "Multiplayer: " + limit(label, 40);
        }
        if (minecraft.hasSingleplayerServer()) {
            return "Singleplayer";
        }
        if (minecraft.level == null) {
            return "Im Menü";
        }
        return "Ingame";
    }

    private static String formatRate(double value, String suffix) {
        if (value >= 1_000_000_000) return Long.toString(Math.round(value / 1_000_000_000.0)) + "b " + suffix;
        if (value >= 1_000_000) return Long.toString(Math.round(value / 1_000_000.0)) + "m " + suffix;
        if (value >= 1_000) return Long.toString(Math.round(value / 1_000.0)) + "k " + suffix;
        return Long.toString(Math.round(value)) + " " + suffix;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) return "";
        String stripped = value.strip();
        return stripped.substring(0, 1).toUpperCase(Locale.ROOT) + stripped.substring(1);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first.strip();
        if (second != null && !second.isBlank()) return second.strip();
        return fallback;
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String limit(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 1) + "…";
    }

    private static String normalizeApplicationId(String value) {
        return value == null ? "" : value.strip();
    }

    private static String resolveApplicationId(VisotarisConfig cfg) {
        String override = normalizeApplicationId(cfg.discordApplicationId);
        if (!override.isEmpty()) return override;
        return normalizeApplicationId(VisotarisConst.DISCORD_APPLICATION_ID);
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "Visotaris-DiscordRPC");
            thread.setDaemon(true);
            return thread;
        }
    }

    private record PresenceText(String details, String state, String smallImage, String smallText) {}
}
