package systems.diath.visotaris_opmod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.MarketHistoryApiClient;
import systems.diath.visotaris_opmod.model.PriceHistory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ConcurrentHashMap<String, Boolean> warming = new ConcurrentHashMap<>();
    private final ExecutorService warmupExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "visotaris-history-warmup");
        thread.setDaemon(true);
        return thread;
    });

    public PriceHistoryCache(MarketHistoryApiClient apiClient) {
        this.apiClient = apiClient;
        this.cache = Caffeine.newBuilder()
            .maximumSize(200)
            // Besonders die stündliche Reihe verändert sich. Ein Verlauf darf
            // nicht bis zum Client-Neustart eingefroren bleiben.
            .expireAfterWrite(5, TimeUnit.MINUTES)
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

    /** Lädt den Verlauf bewusst neu und ersetzt einen ggf. noch frischen Eintrag. */
    public PriceHistory refresh(String materialKey) {
        invalidate(materialKey);
        return get(materialKey);
    }

    /** Liefert ausschließlich bereits bekannte Daten – ohne Netzwerkzugriff. */
    public PriceHistory getCached(String materialKey) {
        PriceHistory cached = cache.getIfPresent(materialKey);
        return cached != null ? cached : PriceHistory.empty();
    }

    /**
     * Wärmt einen Verlauf im Hintergrund vor. Höchstens zwei Abrufe laufen
     * parallel; mehrfache UI-Polls starten für dasselbe Item keinen zweiten Abruf.
     */
    public void warm(String materialKey) {
        if (cache.getIfPresent(materialKey) != null || warming.putIfAbsent(materialKey, Boolean.TRUE) != null) return;
        warmupExecutor.execute(() -> {
            try { get(materialKey); }
            finally { warming.remove(materialKey); }
        });
    }
}
