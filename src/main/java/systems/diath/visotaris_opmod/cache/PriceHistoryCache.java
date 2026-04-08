package systems.diath.visotaris_opmod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.MarketHistoryApiClient;
import systems.diath.visotaris_opmod.model.PriceHistoryPoint;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * In-Memory-Cache für Preisverlauf-Daten pro Material.
 *
 * Lädt Daten lazy beim ersten Zugriff via {@link MarketHistoryApiClient}.
 * Maximal 200 Einträge (unterschiedliche Materialien) werden gehalten;
 * älteste Einträge werden automatisch verdrängt.
 */
public final class PriceHistoryCache {

    private final Cache<String, List<PriceHistoryPoint>> cache;
    private final MarketHistoryApiClient apiClient;

    public PriceHistoryCache(MarketHistoryApiClient apiClient) {
        this.apiClient = apiClient;
        this.cache = Caffeine.newBuilder()
            .maximumSize(200)
            .build();
    }

    /**
     * Gibt den Preisverlauf für ein Material zurück.
     * Ist kein Eintrag im Cache, wird die API synchron angefragt.
     *
     * @param materialKey Item-Key in lowercase (z.B. {@code "diamond"})
     * @return Liste der Preisverlauf-Datenpunkte, leer bei Fehler
     */
    public List<PriceHistoryPoint> get(String materialKey) {
        List<PriceHistoryPoint> cached = cache.getIfPresent(materialKey);
        if (cached != null) return cached;

        try {
            List<PriceHistoryPoint> fetched = apiClient.fetchHistory(materialKey);
            if (!fetched.isEmpty()) {
                cache.put(materialKey, fetched);
            }
            return fetched;
        } catch (IOException e) {
            VisotarisLogger.warn("PriceHistoryCache: Fetch fehlgeschlagen für '{}': {}", materialKey, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Entfernt einen Eintrag aus dem Cache (erzwingt Neu-Fetch beim nächsten Zugriff). */
    public void invalidate(String materialKey) {
        cache.invalidate(materialKey);
    }

    /** Entfernt alle Einträge aus dem Cache. */
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
