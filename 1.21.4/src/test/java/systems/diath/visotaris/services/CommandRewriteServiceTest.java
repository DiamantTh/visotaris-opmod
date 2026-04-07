package systems.diath.visotaris.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import systems.diath.visotaris.config.TestConfigManager;
import systems.diath.visotaris.config.VisotarisConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandRewriteServiceTest {

    private VisotarisConfig cfg;
    private CommandRewriteService service;

    @BeforeEach
    void setUp() {
        cfg     = new VisotarisConfig();
        service = new CommandRewriteService(TestConfigManager.of(cfg));
    }

    // ── Kurzformerweiterung ───────────────────────────────────────────────────

    @ParameterizedTest(name = "''{0}'' → ''{1}''")
    @CsvSource(delimiterString = "->", value = {
        "pay DiamantTh 1k              -> pay DiamantTh 1000",
        "pay DiamantTh 1.5k            -> pay DiamantTh 1500",
        "pay DiamantTh 2m              -> pay DiamantTh 2000000",
        "pay DiamantTh 1b              -> pay DiamantTh 1000000000",
        "bank einzahlen 500k           -> bank einzahlen 500000",
        "bank auszahlen 1.5m           -> bank auszahlen 1500000",
        "sell item 250k                -> sell item 250000",
        "auction startprice 100k       -> auction startprice 100000",
        "bid 75k                       -> bid 75000",
    })
    void shortformsAreExpanded(String input, String expected) {
        assertEquals(expected.strip(), service.rewrite(input.strip()));
    }

    @Test
    void leadingSlashIsToleratedAndExpanded() {
        // trimStart('/') in rewrite() lässt auch /pay durch
        assertEquals("/pay DiamantTh 1000", service.rewrite("/pay DiamantTh 1k"));
    }

    // ── Kein Rewrite für unbekannte Commands ──────────────────────────────────

    @ParameterizedTest(name = "''{0}'' bleibt unverändert")
    @CsvSource({
        "give player 5k",
        "tp player2",
        "gamemode creative",
        "msg player 1k Glueckwunsch",
    })
    void unknownCommandsAreUnchanged(String input) {
        assertEquals(input, service.rewrite(input));
    }

    // ── Feature deaktiviert ───────────────────────────────────────────────────

    @Test
    void disabledConfigSkipsRewrite() {
        cfg.enableCommandShortforms = false;
        assertEquals("pay DiamantTh 5k", service.rewrite("pay DiamantTh 5k"));
    }

    // ── Kantenfall: kein Suffix ───────────────────────────────────────────────

    @Test
    void plainNumberIsUnchanged() {
        assertEquals("pay DiamantTh 5000", service.rewrite("pay DiamantTh 5000"));
    }
}
