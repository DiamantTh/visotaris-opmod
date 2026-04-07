package systems.diath.visotaris_opmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.visotaris_opmod.VisotarisLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lädt und speichert die Mod-Konfiguration als JSON-Datei.
 *
 * Config-Backend und Config-UI sind bewusst getrennt:
 *   - ConfigManager  → Persistenz (diese Klasse)
 *   - ConfigScreen   → UI (separate Klasse, Phase 3)
 *
 * Bei fehlendem oder korruptem File werden die Defaults aus {@link VisotarisConfig} verwendet.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("visotaris.json");

    private VisotarisConfig config = new VisotarisConfig();

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            VisotarisLogger.info("Neue Konfiguration erstellt: {}", configPath);
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            VisotarisConfig loaded = GSON.fromJson(reader, VisotarisConfig.class);
            if (loaded != null) config = loaded;
            VisotarisLogger.info("Konfiguration geladen von: {}", configPath);
        } catch (IOException e) {
            VisotarisLogger.warn("Konfiguration konnte nicht gelesen werden, nutze Defaults: {}", e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            VisotarisLogger.error("Konfiguration konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }

    /** Löscht die Konfigdatei und lädt Defaults neu (Reset-Funktion). */
    public void reset() {
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            VisotarisLogger.error("Konfigdatei konnte nicht gelöscht werden: {}", e.getMessage());
        }
        config = new VisotarisConfig();
        save();
        VisotarisLogger.info("Konfiguration zurückgesetzt.");
    }

    public VisotarisConfig getConfig() {
        return config;
    }
}
