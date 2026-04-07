package systems.diath.visotaris.cache;

import systems.diath.visotaris.model.MarketPrice;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-sicherer In-Memory-Cache für Marktpreise.
 *
 * Die interne Map wird atomar ausgetauscht, sodass Lese-Threads nie auf
 * einer halbfertigen Map arbeiten. Dieses Muster vermeidet explizites Locking
 * für den häufigen Lesepfad (Tooltip-Rendering, Inventarbewertung).
 */
public final class MarketCache {

    private final AtomicReference<Map<String, MarketPrice>> data =
        new AtomicReference<>(Collections.emptyMap());

    /**
     * Überschreibt den gesamten Cache atomar mit frischen API-Daten.
     */
    public void update(List<MarketPrice> prices) {
        Map<String, MarketPrice> map = new HashMap<>(prices.size() * 2);
        for (MarketPrice p : prices) {
            map.put(p.getItemKey(), p);
        }
        data.set(Collections.unmodifiableMap(map));
    }

    public Optional<MarketPrice> get(String itemKey) {
        return Optional.ofNullable(data.get().get(itemKey.toLowerCase()));
    }

    public Map<String, MarketPrice> snapshot() {
        return data.get();
    }

    public boolean isEmpty() {
        return data.get().isEmpty();
    }
}
