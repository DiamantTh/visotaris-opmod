package systems.diath.visotaris_opmod.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Generisches Disk-Persistence-Hilfsmittel für JSON-Cache-Dateien.
 *
 * <p>Format:
 * <pre>
 * {
 *   "meta": {
 *     "savedAt":    "2026-04-08T14:32:11Z",   // ISO-8601 – nur für Menschen
 *     "savedAtMs":  1744122731000,             // Epoch-ms – für isStale()-Prüfung
 *     "source":     "https://...",
 *     "entryCount": 312,
 *     "modVersion": "0.3.0+mc1.21.11"
 *   },
 *   "entries": [ ... ]
 * }
 * </pre>
 *
 * @param <T> Typ der einzelnen Cache-Einträge (z.B. {@code MarketPrice})
 */
public final class JsonCachePersistence<T> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter ISO_FMT =
        DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final String sourceUrl;
    private final Type   listType;

    public JsonCachePersistence(String sourceUrl, Type listType) {
        this.sourceUrl = sourceUrl;
        this.listType  = listType;
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────

    /** Geladene Disk-Daten: Einträge + Zeitpunkt des Speicherns (Epoch-ms). */
    public static final class DiskSnapshot<T> {
        public final List<T> entries;
        public final long    savedAtMs;

        public DiskSnapshot(List<T> entries, long savedAtMs) {
            this.entries   = entries;
            this.savedAtMs = savedAtMs;
        }
    }

    // ── Schreiben ─────────────────────────────────────────────────────────────

    /**
     * Schreibt {@code entries} mit Meta-Block als Pretty-JSON nach {@code file}.
     * Das Elternverzeichnis wird automatisch angelegt.
     */
    public void save(File file, List<T> entries, long savedAtMs) {
        try {
            file.getParentFile().mkdirs();

            String modVer = FabricLoader.getInstance()
                .getModContainer(VisotarisConst.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");

            JsonObject meta = new JsonObject();
            meta.addProperty("savedAt",    ISO_FMT.format(Instant.ofEpochMilli(savedAtMs)));
            meta.addProperty("savedAtMs",  savedAtMs);
            meta.addProperty("source",     sourceUrl);
            meta.addProperty("entryCount", entries.size());
            meta.addProperty("modVersion", modVer);

            JsonObject root = new JsonObject();
            root.add("meta",    meta);
            root.add("entries", GSON.toJsonTree(entries, listType));

            try (Writer w = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            VisotarisLogger.debug("Disk-Cache geschrieben: {} ({} Einträge).",
                file.getName(), entries.size());
        } catch (IOException e) {
            VisotarisLogger.warn("Disk-Cache konnte nicht geschrieben werden ({}): {}",
                file.getName(), e.getMessage());
        }
    }

    // ── Lesen ─────────────────────────────────────────────────────────────────

    /**
     * Liest {@code file} und gibt ein {@link DiskSnapshot} zurück.
     * Gibt {@code null} zurück wenn die Datei fehlt oder nicht lesbar ist.
     */
    public DiskSnapshot<T> load(File file) {
        if (!file.exists()) return null;
        try (Reader r = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {

            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return null;

            long savedAtMs = 0L;
            JsonElement metaEl = root.get("meta");
            if (metaEl != null && metaEl.isJsonObject()) {
                JsonElement ts = metaEl.getAsJsonObject().get("savedAtMs");
                if (ts != null) savedAtMs = ts.getAsLong();
            }

            JsonElement entriesEl = root.get("entries");
            if (entriesEl == null) return null;

            List<T> entries = GSON.fromJson(entriesEl, listType);
            return new DiskSnapshot<>(
                entries != null ? entries : Collections.emptyList(),
                savedAtMs
            );
        } catch (Exception e) {
            VisotarisLogger.warn("Disk-Cache konnte nicht gelesen werden ({}): {}",
                file.getName(), e.getMessage());
            return null;
        }
    }
}
