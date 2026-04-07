package systems.diath.visotaris.services;

import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.api.MarketApiClient;
import systems.diath.visotaris.cache.MarketCache;
import systems.diath.visotaris.config.ConfigManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodischer Hintergrundfetcher für Marktpreise.
 * Trennt HTTP-Transport (MarketApiClient) von Cache (MarketCache) strikt.
 */
public final class MarketSyncService {

    private final MarketCache       cache;
    private final ConfigManager     config;
    private final MarketApiClient   client;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "visotaris-market-sync");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> task;

    public MarketSyncService(MarketCache cache, ConfigManager config) {
        this.cache   = cache;
        this.config  = config;
        this.client  = new MarketApiClient(config);
    }

    public void start() {
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
            VisotarisLogger.debug("MarketCache aktualisiert: {} Einträge.", prices.size());
        } catch (IOException e) {
            VisotarisLogger.warn("Markt-API nicht erreichbar: {}", e.getMessage());
        }
    }
}
