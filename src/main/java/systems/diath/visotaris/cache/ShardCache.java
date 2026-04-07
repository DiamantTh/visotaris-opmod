package systems.diath.visotaris.cache;

import systems.diath.visotaris.model.ShardRate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-sicherer In-Memory-Cache für Merchant-/Shardkurse.
 * Schüssel sind bereits normalisiert (via {@code MerchantApiClient.normalizeSource}).
 */
public final class ShardCache {

    private final AtomicReference<Map<String, ShardRate>> data =
        new AtomicReference<>(Collections.emptyMap());
    private final AtomicLong lastUpdatedMs = new AtomicLong(0L);

    public void update(List<ShardRate> rates) {
        Map<String, ShardRate> map = new HashMap<>(rates.size() * 2);
        for (ShardRate r : rates) {
            map.put(r.getSource(), r);
        }
        data.set(Collections.unmodifiableMap(map));
        lastUpdatedMs.set(System.currentTimeMillis());
    }

    public Optional<ShardRate> get(String normalizedSource) {
        return Optional.ofNullable(data.get().get(normalizedSource));
    }

    public Map<String, ShardRate> snapshot() {
        return data.get();
    }

    public boolean isEmpty() {
        return data.get().isEmpty();
    }

    /** Millisekunden seit letztem erfolgreichem Update, oder {@code Long.MAX_VALUE} wenn nie aktualisiert. */
    public long getAgeSeconds() {
        long last = lastUpdatedMs.get();
        if (last == 0L) return Long.MAX_VALUE;
        return (System.currentTimeMillis() - last) / 1000L;
    }

    /** {@code true} wenn die Daten älter als {@code maxAgeSeconds} Sekunden sind oder noch nie geladen wurden. */
    public boolean isStale(long maxAgeSeconds) {
        return getAgeSeconds() > maxAgeSeconds;
    }
}
