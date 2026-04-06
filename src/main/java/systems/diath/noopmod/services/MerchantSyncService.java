package systems.diath.noopmod.services;

import systems.diath.noopmod.NoOpLogger;
import systems.diath.noopmod.api.MerchantApiClient;
import systems.diath.noopmod.cache.ShardCache;
import systems.diath.noopmod.config.ConfigManager;

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
    private final MerchantApiClient client = new MerchantApiClient();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "noopmod-merchant-sync");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> task;

    public MerchantSyncService(ShardCache cache, ConfigManager config) {
        this.cache  = cache;
        this.config = config;
    }

    public void start() {
        int intervalSec = config.getConfig().merchantRefreshIntervalSeconds;
        task = scheduler.scheduleAtFixedRate(this::fetch, 0, intervalSec, TimeUnit.SECONDS);
        NoOpLogger.info("MerchantSyncService gestartet (Intervall: {}s).", intervalSec);
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
            NoOpLogger.debug("ShardCache aktualisiert: {} Einträge.", rates.size());
        } catch (IOException e) {
            NoOpLogger.warn("Merchant-API nicht erreichbar: {}", e.getMessage());
        }
    }
}
