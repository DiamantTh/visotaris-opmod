package systems.diath.visotaris_opmod.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.reflect.TypeToken;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.model.MarketPrice;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-sicherer In-Memory-Cache für Marktpreise.
 *
 * <p>Intern auf Caffeine umgestellt: begrenzt den Speicherbedarf (max. 10 000 Einträge)
 * und ermöglicht sauberes Eviction-Handling ohne manuelles Locking.
 * Disk-Persistenz via {@link JsonCachePersistence}: lesbare JSON-Datei mit
 * Meta-Block (Zeitstempel, Quelle, Version).
 */
public final class MarketCache {

    private static final String SOURCE_URL = "https://api.opsucht.net/market/prices";

    private final Cache<String, MarketPrice> caffeine = Caffeine.newBuilder()
        .maximumSize(10_000)
        .build();

    private final AtomicLong lastUpdatedMs = new AtomicLong(0L);

    private final JsonCachePersistence<MarketPrice> persistence =
        new JsonCachePersistence<>(SOURCE_URL,
            new TypeToken<List<MarketPrice>>() {}.getType());

    // ── Schreiben ────────────────────────────────────────────────────────────

    /** Überschreibt den gesamten Cache mit frischen API-Daten. */
    public void update(List<MarketPrice> prices) {
        caffeine.invalidateAll();
        for (MarketPrice p : prices) {
            caffeine.put(p.getItemKey(), p);
        }
        lastUpdatedMs.set(System.currentTimeMillis());
    }

    // ── Lesen ────────────────────────────────────────────────────────────────

    public Optional<MarketPrice> get(String itemKey) {
        return Optional.ofNullable(caffeine.getIfPresent(itemKey.toLowerCase()));
    }

    public Map<String, MarketPrice> snapshot() {
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

    // ── Disk-Persistenz ──────────────────────────────────────────────────────

    /** Schreibt den aktuellen Cache-Inhalt als JSON nach {@code file}. */
    public void saveToDisk(File file) {
        long ts = lastUpdatedMs.get();
        if (ts == 0L) return;
        persistence.save(file, new ArrayList<>(caffeine.asMap().values()), ts);
    }

    /**
     * Lädt Cache-Daten von {@code file} als Warm-Start.
     * Aktualisiert {@code lastUpdatedMs} auf den gespeicherten Zeitstempel.
     * Bei fehlendem oder defektem File passiert nichts.
     */
    public void loadFromDisk(File file) {
        var snapshot = persistence.load(file);
        if (snapshot == null || snapshot.entries.isEmpty()) return;
        caffeine.invalidateAll();
        for (MarketPrice p : snapshot.entries) {
            if (p != null && p.getItemKey() != null) {
                caffeine.put(p.getItemKey(), p);
            }
        }
        lastUpdatedMs.set(snapshot.savedAtMs);
        VisotarisLogger.info("MarketCache: {} Einträge aus Disk-Cache geladen (Alter: {}s).",
            snapshot.entries.size(), getAgeSeconds());
    }
}
