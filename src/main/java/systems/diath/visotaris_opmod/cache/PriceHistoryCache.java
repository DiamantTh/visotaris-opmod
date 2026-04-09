package systems.diath.visotaris_opmod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.MarketHistoryApiClient;
import systems.diath.visotaris_opmod.model.PriceHistory;

import java.io.IOException;

/**
 * In-Memory-Cache für Preisverlauf-Daten pro Material.
 *
 * Lädt Daten lazy beim ersten Zugriff via {@link MarketHistoryApiClient}.
 * Maximal 200 Einträge (unterschiedliche Materialien) werden gehalten;
 * älteste Einträge werden automatisch verdrängt.
 */
public final class PriceHistoryCache {

    private final Cache<String, PriceHistory> cache;
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
     * @return {@link PriceHistory} mit allen Granularitäten, leer bei Fehler
     */
    public PriceHistory get(String materialKey) {
        PriceHistory cached = cache.getIfPresent(materialKey);
        if (cached != null) return cached;

        try {
            PriceHistory fetched = apiClient.fetchHistory(materialKey);
            if (fetched != null) cache.put(materialKey, fetched);
            return fetched != null ? fetched : PriceHistory.empty();
        } catch (IOException e) {
            VisotarisLogger.warn("PriceHistoryCache: Fetch fehlgeschlagen für '{}': {}", materialKey, e.getMessage());
            return PriceHistory.empty();
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
