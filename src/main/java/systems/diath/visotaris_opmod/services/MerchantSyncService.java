package systems.diath.visotaris_opmod.services;

import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.MerchantApiClient;
import systems.diath.visotaris_opmod.cache.ShardCache;
import systems.diath.visotaris_opmod.config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodischer Hintergrundfetcher für Merchant-/Shardkurse.
 *
 * <p>Beim Start wird der JSON-Disk-Cache als Warm-Start geladen. Nach jedem
 * erfolgreichen Fetch wird der Cache automatisch auf Disk geschrieben.
 */
public final class MerchantSyncService {

    private final ShardCache              cache;
    private final ConfigManager           config;
    private final MerchantApiClient       client;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "visotaris-merchant-sync");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> task;
    private File diskFile;

    private static final long   MIN_MANUAL_INTERVAL_MS = 60_000L;
    private final AtomicLong    lastManualRefreshMs    = new AtomicLong(0L);

    public MerchantSyncService(ShardCache cache, ConfigManager config) {
        this.cache   = cache;
        this.config  = config;
        this.client  = new MerchantApiClient(config);
    }

    public void start() {
        diskFile = new File(VisotarisConst.getCacheDir("json"), "shard.json");
        cache.loadFromDisk(diskFile);

        int intervalSec = config.getConfig().merchantRefreshIntervalSeconds;
        task = scheduler.scheduleAtFixedRate(this::scheduledFetch, 0, intervalSec, TimeUnit.SECONDS);
        VisotarisLogger.info("MerchantSyncService gestartet (Intervall: {}s).", intervalSec);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
    }

    public void refresh() {
        long now  = System.currentTimeMillis();
        long last = lastManualRefreshMs.get();
        if (now - last < MIN_MANUAL_INTERVAL_MS) {
            VisotarisLogger.debug("MerchantSyncService: refresh() übersprungen (Cooldown {}s).",
                    (MIN_MANUAL_INTERVAL_MS - (now - last)) / 1000);
            return;
        }
        lastManualRefreshMs.set(now);
        scheduler.execute(this::fetch);
    }

    private void scheduledFetch() {
        lastManualRefreshMs.set(System.currentTimeMillis());
        fetch();
    }

    private void fetch() {
        try {
            var rates = client.fetchRates();
            cache.update(rates);
            cache.saveToDisk(diskFile);
            VisotarisLogger.debug("ShardCache aktualisiert: {} Einträge.", rates.size());
        } catch (IOException e) {
            VisotarisLogger.warn("Merchant-API nicht erreichbar: {}", e.getMessage());
        }
    }
}
