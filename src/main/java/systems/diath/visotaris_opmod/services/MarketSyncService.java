package systems.diath.visotaris_opmod.services;

import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.MarketApiClient;
import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodischer Hintergrundfetcher für Marktpreise.
 * Trennt HTTP-Transport (MarketApiClient) von Cache (MarketCache) strikt.
 *
 * <p>Beim Start wird der JSON-Disk-Cache als Warm-Start geladen (– kein API-Call
 * nötig wenn die Daten noch frisch genug sind). Nach jedem erfolgreichen Fetch
 * wird der Cache automatisch auf Disk geschrieben.
 */
public final class MarketSyncService {

    private final MarketCache             cache;
    private final ConfigManager           config;
    private final MarketApiClient         client;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "visotaris-market-sync");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> task;
    private File diskFile;

    /**
     * Minimaler Abstand zwischen manuell ausgelösten Fetches (ms).
     * Schützt die OPSUCHT-API vor Hammering durch schnelles Container-Öffnen
     * oder mehrfaches Button-Drücken. Der periodische Timer ist davon unberührt.
     */
    private static final long   MIN_MANUAL_INTERVAL_MS = 60_000L;  // 60 s
    private final AtomicLong    lastManualRefreshMs    = new AtomicLong(0L);

    public MarketSyncService(MarketCache cache, ConfigManager config) {
        this.cache   = cache;
        this.config  = config;
        this.client  = new MarketApiClient(config);
    }

    public void start() {
        diskFile = new File(VisotarisConst.getCacheDir("json"), "market.json");
        cache.loadFromDisk(diskFile);

        int intervalSec = config.getConfig().marketRefreshIntervalSeconds;
        task = scheduler.scheduleAtFixedRate(this::scheduledFetch, 0, intervalSec, TimeUnit.SECONDS);
        VisotarisLogger.info("MarketSyncService gestartet (Intervall: {}s).", intervalSec);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    /** Direkt-Fetch (z.B. per Client-Command oder Button auslösbar).
     *  Rate-limitiert: maximal alle {@value #MIN_MANUAL_INTERVAL_MS} ms. */
    public void refresh() {
        long now  = System.currentTimeMillis();
        long last = lastManualRefreshMs.get();
        if (now - last < MIN_MANUAL_INTERVAL_MS) {
            VisotarisLogger.debug("MarketSyncService: refresh() übersprungen (Cooldown {}s).",
                    (MIN_MANUAL_INTERVAL_MS - (now - last)) / 1000);
            return;
        }
        lastManualRefreshMs.set(now);
        scheduler.execute(this::fetch);
    }

    /** Erzwingt einen sofortigen Fetch ohne Cooldown-Prüfung (interner Timer-Aufruf). */
    private void scheduledFetch() {
        lastManualRefreshMs.set(System.currentTimeMillis()); // Timer-Fetch zählt als Cooldown-Reset
        fetch();
    }

    private void fetch() {
        try {
            var prices = client.fetchPrices();
            cache.update(prices);
            cache.saveToDisk(diskFile);
            VisotarisLogger.debug("MarketCache aktualisiert: {} Einträge.", prices.size());
        } catch (IOException e) {
            VisotarisLogger.warn("Markt-API nicht erreichbar: {}", e.getMessage());
        }
    }
}
