package systems.diath.visotaris.cache;

import systems.diath.visotaris.model.ShardRate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-sicherer In-Memory-Cache für Merchant-/Shardkurse.
 * Schüssel sind bereits normalisiert (via {@code MerchantApiClient.normalizeSource}).
 */
public final class ShardCache {

    private final AtomicReference<Map<String, ShardRate>> data =
        new AtomicReference<>(Collections.emptyMap());

    public void update(List<ShardRate> rates) {
        Map<String, ShardRate> map = new HashMap<>(rates.size() * 2);
        for (ShardRate r : rates) {
            map.put(r.getSource(), r);
        }
        data.set(Collections.unmodifiableMap(map));
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
}
