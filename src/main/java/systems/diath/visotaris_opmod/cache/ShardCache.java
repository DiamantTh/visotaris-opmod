package systems.diath.visotaris_opmod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.reflect.TypeToken;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.model.ShardRate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-sicherer In-Memory-Cache für Merchant-/Shardkurse.
 * Schlüssel sind bereits normalisiert (via {@code MerchantApiClient.normalizeSource}).
 *
 * <p>Intern auf Caffeine umgestellt; Disk-Persistenz via {@link JsonCachePersistence}.
 */
public final class ShardCache {

    private static final String SOURCE_URL = "https://api.opsucht.net/merchant/rates";

    private final Cache<String, ShardRate> caffeine = Caffeine.newBuilder()
        .maximumSize(1_000)
        .build();

    private final AtomicLong lastUpdatedMs = new AtomicLong(0L);

    private final JsonCachePersistence<ShardRate> persistence =
        new JsonCachePersistence<>(SOURCE_URL,
            new TypeToken<List<ShardRate>>() {}.getType());

    // ── Schreiben ─────────────────────────────────────────────────────────────

    public void update(List<ShardRate> rates) {
        caffeine.invalidateAll();
        for (ShardRate r : rates) {
            caffeine.put(r.getSource(), r);
        }
        lastUpdatedMs.set(System.currentTimeMillis());
    }

    // ── Lesen ─────────────────────────────────────────────────────────────────

    public Optional<ShardRate> get(String normalizedSource) {
        return Optional.ofNullable(caffeine.getIfPresent(normalizedSource));
    }

    public Map<String, ShardRate> snapshot() {
        return Collections.unmodifiableMap(caffeine.asMap());
    }

    public boolean isEmpty() {
        return caffeine.asMap().isEmpty();
    }

    /** Sekunden seit letztem erfolgreichem Update, oder {@code Long.MAX_VALUE} wenn nie. */
    public long getAgeSeconds() {
        long last = lastUpdatedMs.get();
        if (last == 0L) return Long.MAX_VALUE;
        return (System.currentTimeMillis() - last) / 1000L;
    }

    /** {@code true} wenn Daten älter als {@code maxAgeSeconds} Sekunden oder noch nie geladen. */
    public boolean isStale(long maxAgeSeconds) {
        return getAgeSeconds() > maxAgeSeconds;
    }

    // ── Disk-Persistenz ───────────────────────────────────────────────────────

    /** Schreibt den aktuellen Cache-Inhalt als JSON nach {@code file}. */
    public void saveToDisk(File file) {
        long ts = lastUpdatedMs.get();
        if (ts == 0L) return;
        persistence.save(file, new ArrayList<>(caffeine.asMap().values()), ts);
    }

    /**
     * Lädt Cache-Daten von {@code file} als Warm-Start.
     * Bei fehlendem oder defektem File passiert nichts.
     */
    public void loadFromDisk(File file) {
        var snapshot = persistence.load(file);
        if (snapshot == null || snapshot.entries.isEmpty()) return;
        caffeine.invalidateAll();
        for (ShardRate r : snapshot.entries) {
            if (r != null && r.getSource() != null) {
                caffeine.put(r.getSource(), r);
            }
        }
        lastUpdatedMs.set(snapshot.savedAtMs);
        VisotarisLogger.info("ShardCache: {} Einträge aus Disk-Cache geladen (Alter: {}s).",
            snapshot.entries.size(), getAgeSeconds());
    }
}
