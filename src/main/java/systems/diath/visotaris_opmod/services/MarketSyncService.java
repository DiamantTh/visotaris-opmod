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

    public MarketSyncService(MarketCache cache, ConfigManager config) {
        this.cache   = cache;
        this.config  = config;
        this.client  = new MarketApiClient(config);
    }

    public void start() {
        diskFile = new File(VisotarisConst.getCacheDir("json"), "market.json");
        cache.loadFromDisk(diskFile);

        int intervalSec = config.getConfig().marketRefreshIntervalSeconds;
        task = scheduler.scheduleAtFixedRate(this::fetch, 0, intervalSec, TimeUnit.SECONDS);
        VisotarisLogger.info("MarketSyncService gestartet (Intervall: {}s).", intervalSec);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    /** Direkt-Fetch (z.B. per Client-Command auslösbar). */
    public void refresh() {
        scheduler.execute(this::fetch);
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
