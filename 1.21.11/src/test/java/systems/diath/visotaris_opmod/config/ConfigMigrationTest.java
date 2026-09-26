package systems.diath.visotaris_opmod.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @TempDir Path directory;

    @Test void migratesGermanSectionsWithoutLosingValues() throws Exception {
        Path file = directory.resolve("visotaris.toml");
        Files.writeString(file, "marketRefreshIntervalSeconds = 330\n\n[anzeige]\nshowMarketTooltips = false\nshowHud = false\n\n[netzwerk]\nwebUiPort = 7788\n\n[modus]\nobserverModeOnly = true\n");
        ConfigManager manager = new ConfigManager(file);
        manager.load();
        assertFalse(manager.getConfig().showMarketTooltips);
        assertFalse(manager.getConfig().showHud);
        assertEquals(7788, manager.getConfig().webUiPort);
        assertEquals(330, manager.getConfig().marketRefreshIntervalSeconds);
        assertTrue(manager.getConfig().observerModeOnly);
        manager.getConfig().tooltipShowBuyPrice = false;
        PriceAlertRule rule = new PriceAlertRule("diamond", "BUY_ABOVE", 200);
        rule.repeat = true; rule.notification = "WEB";
        manager.getConfig().priceAlertRules.add(rule);
        manager.save();
        String migrated = Files.readString(file);
        assertTrue(migrated.contains("[display]"), migrated);
        assertTrue(migrated.contains("[network]"));
        assertTrue(migrated.contains("[tooltips]"));
        assertFalse(migrated.contains("[anzeige]"));
        assertFalse(migrated.contains("[netzwerk]"));
        assertFalse(manager.getConfig().tooltipShowBuyPrice);
        ConfigManager reloaded = new ConfigManager(file);
        reloaded.load();
        assertEquals(1, reloaded.getConfig().priceAlertRules.size());
        assertEquals("WEB", reloaded.getConfig().priceAlertRules.getFirst().notification);
        assertEquals(330, reloaded.getConfig().marketRefreshIntervalSeconds);
    }

    @Test void migratesLegacyChatChannelsWithoutLosingAlertState() throws Exception {
        Path file = directory.resolve("alerts.toml");
        PriceAlertRule chat = new PriceAlertRule("diamond", "BUY_ABOVE", 250);
        chat.id = "legacy-chat"; chat.notification = "CHAT"; chat.repeat = true;
        chat.cooldownSeconds = 720; chat.lastTriggeredAtMs = 123456L;
        chat.triggered = true; chat.latched = true;
        PriceAlertRule both = new PriceAlertRule("sand", "SELL_BELOW", 12);
        both.id = "legacy-both"; both.notification = "BOTH"; both.enabled = false;
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String chatJson = gson.toJson(chat.toMap()).replace("HUD", "CHAT");
        String bothJson = gson.toJson(both.toMap()).replace("HUD_WEB", "BOTH");
        Files.writeString(file, "[priceAlerts]\nenabled = true\nrules = [\"" + chatJson.replace("\\", "\\\\").replace("\"", "\\\"") + "\", \"" + bothJson.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]\n");
        ConfigManager manager = new ConfigManager(file);
        manager.load();
        assertEquals(2, manager.getConfig().priceAlertRules.size());
        PriceAlertRule migratedChat = manager.getConfig().priceAlertRules.get(0);
        assertEquals("legacy-chat", migratedChat.id);
        assertEquals("HUD", migratedChat.notification);
        assertEquals(250, migratedChat.threshold);
        assertEquals(720, migratedChat.cooldownSeconds);
        assertEquals(123456L, migratedChat.lastTriggeredAtMs);
        assertTrue(migratedChat.triggered);
        assertTrue(migratedChat.latched);
        assertEquals("HUD_WEB", manager.getConfig().priceAlertRules.get(1).notification);
        assertFalse(manager.getConfig().priceAlertRules.get(1).enabled);
        String rewritten = Files.readString(file);
        assertFalse(rewritten.contains("\\\"CHAT\\\""), rewritten);
        assertFalse(rewritten.contains("\\\"BOTH\\\""), rewritten);
    }
}
