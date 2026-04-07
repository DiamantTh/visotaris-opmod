package systems.diath.visotaris.services;

import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.api.MerchantApiClient;
import systems.diath.visotaris.cache.ShardCache;
import systems.diath.visotaris.config.ConfigManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodischer Hintergrundfetcher für Merchant-/Shardkurse.
 */
public final class MerchantSyncService {

    private final ShardCache        cache;
    private final ConfigManager     config;
    private final MerchantApiClient client;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "visotaris-merchant-sync");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> task;

    public MerchantSyncService(ShardCache cache, ConfigManager config) {
        this.cache   = cache;
        this.config  = config;
        this.client  = new MerchantApiClient(config);
    }

    public void start() {
        int intervalSec = config.getConfig().merchantRefreshIntervalSeconds;
        task = scheduler.scheduleAtFixedRate(this::fetch, 0, intervalSec, TimeUnit.SECONDS);
        VisotarisLogger.info("MerchantSyncService gestartet (Intervall: {}s).", intervalSec);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    public void refresh() {
        scheduler.execute(this::fetch);
    }

    private void fetch() {
        try {
            var rates = client.fetchRates();
            cache.update(rates);
            VisotarisLogger.debug("ShardCache aktualisiert: {} Einträge.", rates.size());
        } catch (IOException e) {
            VisotarisLogger.warn("Merchant-API nicht erreichbar: {}", e.getMessage());
        }
    }
}
