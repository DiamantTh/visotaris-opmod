package systems.diath.visotaris.config;

/**
 * Test-Hilfsmethode: erzeugt einen ConfigManager ohne FabricLoader-Aufruf.
 * Darf nur in Unit-Tests verwendet werden.
 */
public final class TestConfigManager {

    private TestConfigManager() {}

    public static ConfigManager of(VisotarisConfig cfg) {
        return new ConfigManager(cfg);
    }
}
