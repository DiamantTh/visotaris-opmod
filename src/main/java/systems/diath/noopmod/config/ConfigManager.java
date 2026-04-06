package systems.diath.noopmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import systems.diath.noopmod.NoOpLogger;

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
 * Bei fehlendem oder korruptem File werden die Defaults aus {@link NoOpConfig} verwendet.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("noopmod.json");

    private NoOpConfig config = new NoOpConfig();

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            NoOpLogger.info("Neue Konfiguration erstellt: {}", configPath);
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            NoOpConfig loaded = GSON.fromJson(reader, NoOpConfig.class);
            if (loaded != null) config = loaded;
            NoOpLogger.info("Konfiguration geladen von: {}", configPath);
        } catch (IOException e) {
            NoOpLogger.warn("Konfiguration konnte nicht gelesen werden, nutze Defaults: {}", e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            NoOpLogger.error("Konfiguration konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }

    /** Löscht die Konfigdatei und lädt Defaults neu (Reset-Funktion). */
    public void reset() {
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            NoOpLogger.error("Konfigdatei konnte nicht gelöscht werden: {}", e.getMessage());
        }
        config = new NoOpConfig();
        save();
        NoOpLogger.info("Konfiguration zurückgesetzt.");
    }

    public NoOpConfig getConfig() {
        return config;
    }
}
