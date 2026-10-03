package systems.diath.visotaris_opmod.services;

import okhttp3.Call;
import okhttp3.Response;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.AuctionApiClient;
import systems.diath.visotaris_opmod.cache.AuctionCache;
import systems.diath.visotaris_opmod.cache.AuctionCachePersistence;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.Auction;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manual auction snapshots by default; optional live SSE only updates the shared local cache. */
public final class AuctionSyncService {
    private final AuctionCache cache;
    private final ConfigManager config;
    private final AuctionApiClient api;
    private final AuctionEventProcessor eventProcessor;
    private final AuctionCachePersistence persistence = new AuctionCachePersistence();
    private final ExecutorService snapshotExecutor = Executors.newSingleThreadExecutor(r -> daemon(r, "visotaris-auction-snapshot"));
    private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor(r -> daemon(r, "visotaris-auction-stream"));
    private final ScheduledExecutorService persistenceExecutor = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "visotaris-auction-cache"));
    private final AtomicBoolean snapshotInFlight = new AtomicBoolean();
    private final AtomicBoolean streamWorkerActive = new AtomicBoolean();
    private final java.util.Set<String> loggedEventTypes = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Object resetReconcileLock = new Object();
    private final java.util.Queue<ServerSentEventParser.Message> eventsDuringReset = new java.util.ArrayDeque<>();
    private boolean resetSnapshotInFlight;
    private boolean resetRequestedAgain;
    private volatile boolean started;
    private volatile boolean streamDesired;
    private volatile boolean streamConnected;
    private volatile Call activeStreamCall;
    private volatile int reconnectSeconds = 2;
    private ScheduledFuture<?> pendingCacheWrite;

    public AuctionSyncService(AuctionCache cache, ConfigManager config) {
        this.cache = cache;
        this.config = config;
        this.api = new AuctionApiClient(config);
        this.eventProcessor = new AuctionEventProcessor(cache);
        persistence.loadInto(cache);
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    /** Starts no network activity on a fresh install. Saved live mode/dev override syncs first, then streams. */
    public synchronized void start() {
        if (started) return;
        started = true;
        applyConfig();
    }

    public synchronized void applyConfig() {
        boolean shouldStream = shouldEnableLiveUpdates(config.getConfig().auctionLiveUpdatesEnabled,
            System.getenv("VISOTARIS_AUCTIONS_STREAM_DEV"));
        boolean wasStreaming = streamDesired;
        streamDesired = started && shouldStream;
        if (!streamDesired) {
            Call call = activeStreamCall;
            if (call != null) call.cancel();
        } else if (!wasStreaming) {
            refreshSnapshot();
        }
    }

    public static boolean shouldEnableLiveUpdates(boolean userEnabled, String devEnvironmentValue) {
        return "true".equalsIgnoreCase(devEnvironmentValue == null ? "" : devEnvironmentValue.trim()) || userEnabled;
    }

    public boolean isLiveUpdatesActive() { return streamDesired; }
    public boolean isStreamConnected() { return streamConnected; }
    public boolean isSnapshotLoading() { return snapshotInFlight.get(); }
    public boolean isDevOverrideActive() {
        return "true".equalsIgnoreCase(System.getenv("VISOTARIS_AUCTIONS_STREAM_DEV") == null
            ? "" : System.getenv("VISOTARIS_AUCTIONS_STREAM_DEV").trim());
    }

    public synchronized void stop() {
        started = false;
        streamDesired = false;
        Call call = activeStreamCall;
        if (call != null) call.cancel();
        persistCache();
        snapshotExecutor.shutdownNow();
        streamExecutor.shutdownNow();
        persistenceExecutor.shutdownNow();
    }

    /** Explicit user refresh. Categories and /active are fetched off-thread; UI callers never block. */
    public CompletableFuture<Boolean> refreshSnapshot() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (!started || !snapshotInFlight.compareAndSet(false, true)) {
            result.complete(false);
            return result;
        }
        Call currentStream = activeStreamCall;
        if (currentStream != null) currentStream.cancel();
        try {
            snapshotExecutor.execute(() -> {
                boolean loaded = false;
                try {
                    try { cache.replaceCategories(api.fetchCategories()); }
                    catch (IOException e) { VisotarisLogger.warn("Auction-API Kategorien nicht erreichbar: {}", e.getMessage()); }
                    List<Auction> active = api.fetchActive();
                    cache.replaceActive(active);
                    scheduleCacheWrite();
                    loaded = true;
                } catch (IOException e) {
                    VisotarisLogger.warn("Auction-API active nicht erreichbar: {}", e.getMessage());
                } finally {
                    snapshotInFlight.set(false);
                    result.complete(loaded);
                }
                if (loaded && streamDesired) ensureStreamWorker();
            });
        } catch (java.util.concurrent.RejectedExecutionException stopped) {
            snapshotInFlight.set(false);
            result.complete(false);
        }
        return result;
    }

    private void ensureStreamWorker() {
        if (!started || !streamDesired || !streamWorkerActive.compareAndSet(false, true)) return;
        try { streamExecutor.execute(this::consumeLoop); }
        catch (java.util.concurrent.RejectedExecutionException stopped) { streamWorkerActive.set(false); }
    }

    private void consumeLoop() {
        try {
            while (started && streamDesired && !snapshotInFlight.get() && !Thread.currentThread().isInterrupted()) {
                Call call = api.newStreamCall(cache.getLastEventId());
                activeStreamCall = call;
                try (Response response = call.execute()) {
                    if (!response.isSuccessful() || response.body() == null)
                        throw new IOException("Auction stream status " + response.code());
                    streamConnected = true;
                    reconnectSeconds = 2;
                    VisotarisLogger.info("Auction-SSE verbunden (Accept: text/event-stream).");
                    ServerSentEventParser parser = new ServerSentEventParser();
                    try (BufferedReader reader = new BufferedReader(response.body().charStream())) {
                        String line;
                        while (started && streamDesired && !snapshotInFlight.get() && (line = reader.readLine()) != null) {
                            parser.accept(line).ifPresent(this::handleMessage);
                        }
                    }
                } catch (IOException e) {
                    if (started && streamDesired && !snapshotInFlight.get()) VisotarisLogger.warn("Auction-SSE getrennt: {}", e.getMessage());
                } finally {
                    streamConnected = false;
                    if (activeStreamCall == call) activeStreamCall = null;
                }
                if (started && streamDesired && !snapshotInFlight.get()) waitBeforeReconnect();
            }
        } finally {
            streamWorkerActive.set(false);
            // If enable raced the previous worker's shutdown, start one replacement.
            if (started && streamDesired && !snapshotInFlight.get()) ensureStreamWorker();
        }
    }

    private void handleMessage(ServerSentEventParser.Message message) {
        if (message.event() != null && loggedEventTypes.add(message.event()))
            VisotarisLogger.info("Auction-SSE-Ereignistyp empfangen: {}.", message.event());
        boolean beginReset = false;
        synchronized (resetReconcileLock) {
            if (message.id() != null) cache.setLastEventId(message.id());
            AuctionEventProcessor.Result result = eventProcessor.apply(message.event(), message.data());
            if (result == AuctionEventProcessor.Result.RESET) {
                if (resetSnapshotInFlight) resetRequestedAgain = true;
                else { resetSnapshotInFlight = true; beginReset = true; }
            } else if (resetSnapshotInFlight) {
                eventsDuringReset.add(message);
            } else {
                if (message.id() != null || result != AuctionEventProcessor.Result.IGNORED) scheduleCacheWrite();
            }
        }
        if (beginReset) reloadAfterStreamReset();
    }

    /** Reconcile after reset without closing the live stream; later events are replayed after the snapshot. */
    private void reloadAfterStreamReset() {
        try {
            snapshotExecutor.execute(() -> {
                List<systems.diath.visotaris_opmod.model.AuctionCategory> categories = null;
                List<systems.diath.visotaris_opmod.model.Auction> active = null;
                try { categories = api.fetchCategories(); }
                catch (IOException e) { VisotarisLogger.warn("Auction-API Kategorien nach stream.reset nicht erreichbar: {}", e.getMessage()); }
                try { active = api.fetchActive(); }
                catch (IOException e) { VisotarisLogger.warn("Auction-API active nach stream.reset nicht erreichbar: {}", e.getMessage()); }

                boolean repeatReset;
                synchronized (resetReconcileLock) {
                    if (categories != null) cache.replaceCategories(categories);
                    if (active != null) cache.replaceActive(active);
                    ServerSentEventParser.Message queued;
                    while ((queued = eventsDuringReset.poll()) != null) {
                        if ("stream.reset".equals(queued.event())) continue;
                        AuctionEventProcessor.Result result = eventProcessor.apply(queued.event(), queued.data());
                        if (queued.id() != null || result != AuctionEventProcessor.Result.IGNORED) scheduleCacheWrite();
                    }
                    repeatReset = resetRequestedAgain;
                    resetRequestedAgain = false;
                    resetSnapshotInFlight = repeatReset;
                }
                scheduleCacheWrite();
                if (repeatReset) reloadAfterStreamReset();
            });
        } catch (java.util.concurrent.RejectedExecutionException stopped) {
            synchronized (resetReconcileLock) {
                resetSnapshotInFlight = false;
                resetRequestedAgain = false;
                ServerSentEventParser.Message queued;
                while ((queued = eventsDuringReset.poll()) != null)
                    eventProcessor.apply(queued.event(), queued.data());
            }
        }
    }

    private synchronized void scheduleCacheWrite() {
        if (persistenceExecutor.isShutdown()) return;
        if (pendingCacheWrite != null) pendingCacheWrite.cancel(false);
        try { pendingCacheWrite = persistenceExecutor.schedule(() -> persistence.save(cache), 750, TimeUnit.MILLISECONDS); }
        catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    private void persistCache() { persistence.save(cache); }

    private void waitBeforeReconnect() {
        int remaining = reconnectSeconds;
        reconnectSeconds = Math.min(60, reconnectSeconds * 2);
        while (remaining-- > 0 && started && streamDesired && !Thread.currentThread().isInterrupted()) {
            try { TimeUnit.SECONDS.sleep(1); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
        }
    }
}
