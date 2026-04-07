package systems.diath.visotaris.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.diath.visotaris.config.TestConfigManager;
import systems.diath.visotaris.config.VisotarisConfig;
import systems.diath.visotaris.model.JobSnapshot;

import static org.junit.jupiter.api.Assertions.*;

class JobTrackerServiceTest {

    private VisotarisConfig cfg;
    private JobTrackerService service;

    @BeforeEach
    void setUp() {
        cfg     = new VisotarisConfig();
        service = new JobTrackerService(TestConfigManager.of(cfg));
    }

    // ── Grundlegendes Parsing ─────────────────────────────────────────────────

    @Test
    void detectsJobNameFromBrackets() {
        service.processMessage("[Holzfäller] • Level 5 • XP: 1000 • $500 • 32%");
        assertEquals("holzfäller", service.getSnapshot().getJobName());
    }

    @Test
    void parsesValuesCorrectly() {
        service.processMessage("[Holzfäller] • Level 5 • XP: 1000 • $500 • 32%");
        JobSnapshot snap = service.getSnapshot();
        assertEquals(5,     snap.getLevel());
        assertEquals(32.0,  snap.getPercent(), 0.001);
        // Akkumulation: 1000 / 2.0 = 500
        assertEquals(500.0, snap.getXp(),   0.001);
        // Akkumulation: 500 / 2.0 = 250
        assertEquals(250.0, snap.getMoney(), 0.001);
    }

    @Test
    void parsesGermanThousandsSeparator() {
        // "1.234" im deutschen Format → 1234, dann / 2.0 = 617
        service.processMessage("[Minenarbeiter] Level 10 • XP: 1.234 • $1.500 • 50%");
        JobSnapshot snap = service.getSnapshot();
        assertEquals("minenarbeiter", snap.getJobName());
        assertEquals(617.0,  snap.getXp(),   0.001);
        assertEquals(750.0,  snap.getMoney(), 0.001);
    }

    @Test
    void parsesGermanDecimalComma() {
        // "1.234,56" → 1234.56, dann / 2.0 = 617.28
        service.processMessage("[Fischer] Level 3 • XP: 1.234,56 • $100 • 10%");
        assertEquals(617.28, service.getSnapshot().getXp(), 0.001);
    }

    // ── Akkumulation ──────────────────────────────────────────────────────────

    @Test
    void accumulatesAcrossMultipleMessages() {
        service.processMessage("[Holzfäller]");
        // Erste Werte: XP 100/2 = 50, Money 50/2 = 25
        service.processMessage("Level 1 • XP: 100 • $50 • 10%");
        // Zweite Werte: XP 200/2 = 100, Money 100/2 = 50 → gesamt 150 / 75
        service.processMessage("Level 2 • XP: 200 • $100 • 20%");
        JobSnapshot snap = service.getSnapshot();
        assertEquals(150.0, snap.getXp(),   0.001);
        assertEquals(75.0,  snap.getMoney(), 0.001);
    }

    @Test
    void jobChangeResetsAccumulators() {
        service.processMessage("[Holzfäller]");
        service.processMessage("Level 1 • XP: 100 • $50 • 10%");
        // Job wechselt → Tracking-Reset; Snapshot ist danach leer bis erste neue Wert-Message
        service.processMessage("[Minenarbeiter]");
        // Jetzt Wert-Message schicken damit der neue Job im Snapshot sichtbar wird
        service.processMessage("Level 1 • XP: 50 • $25 • 5%");
        JobSnapshot snap = service.getSnapshot();
        assertEquals("minenarbeiter", snap.getJobName());
        // Nur neue Werte, keine alten akkumuliert
        assertEquals(25.0, snap.getXp(),   0.001);
        assertEquals(12.5, snap.getMoney(), 0.001);
    }

    // ── Filterregeln ─────────────────────────────────────────────────────────

    @Test
    void ignoresGuillemet() {
        service.processMessage("» [Holzfäller] Level 5 • XP: 1000 • $500 • 32%");
        assertEquals("", service.getSnapshot().getJobName());
    }

    @Test
    void ignoresUnrelatedMessages() {
        service.processMessage("Spieler abc hat den Server betreten.");
        assertEquals("", service.getSnapshot().getJobName());
        assertEquals(0,  service.getSnapshot().getLevel());
    }

    @Test
    void unknownJobNameIsIgnored() {
        service.processMessage("[Zauberer] Level 5 • XP: 100 • $50 • 10%");
        assertEquals("", service.getSnapshot().getJobName());
    }

    // ── Feature deaktiviert ───────────────────────────────────────────────────

    @Test
    void disabledTrackerIgnoresAllMessages() {
        cfg.enableJobTracker = false;
        service.processMessage("[Holzfäller] Level 5 • XP: 1000 • $500 • 32%");
        assertEquals("", service.getSnapshot().getJobName());
    }

    // ── resetTracking ─────────────────────────────────────────────────────────

    @Test
    void manualResetClearsSnapshot() {
        service.processMessage("[Holzfäller] Level 5 • XP: 1000 • $500 • 32%");
        assertNotEquals("", service.getSnapshot().getJobName());
        service.resetTracking();
        assertEquals(0.0, service.getSnapshot().getXp(),   0.001);
        assertEquals(0.0, service.getSnapshot().getMoney(), 0.001);
    }
}
