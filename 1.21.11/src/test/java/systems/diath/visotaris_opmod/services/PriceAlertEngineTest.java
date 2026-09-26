package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.config.PriceAlertRule;
import systems.diath.visotaris_opmod.model.MarketPrice;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PriceAlertEngineTest {
    private final PriceAlertEngine engine = new PriceAlertEngine();
    private MarketPrice price(double buy, double sell) { return new MarketPrice("diamond", buy, sell, 2, 1, "blocks"); }

    @Test void evaluatesPriceAndSpreadBoundaries() {
        assertTrue(PriceAlertEngine.matches("BUY_ABOVE", 101, 100));
        assertTrue(PriceAlertEngine.matches("BUY_BELOW", 99, 100));
        assertTrue(PriceAlertEngine.matches("SELL_ABOVE", 101, 100));
        assertTrue(PriceAlertEngine.matches("SELL_BELOW", 99, 100));
        assertEquals(20, PriceAlertEngine.value("SPREAD_ABOVE", price(120, 100)));
        assertTrue(PriceAlertEngine.matches("SPREAD_ABOVE", 20, 19));
        assertTrue(PriceAlertEngine.matches("SPREAD_BELOW", 20, 21));
    }

    @Test void supportsOneShotCooldownAndRearming() {
        var snapshot = Map.of("diamond", price(120, 80));
        var once = new PriceAlertRule("diamond", "BUY_ABOVE", 100);
        assertEquals(1, engine.evaluate(List.of(once), snapshot, true, 1_000).size());
        assertEquals(0, engine.evaluate(List.of(once), snapshot, true, 2_000).size());
        assertEquals(0, engine.evaluate(List.of(once), Map.of("diamond", price(90, 80)), true, 3_000).size());
        assertEquals(0, engine.evaluate(List.of(once), snapshot, true, 4_000).size()); // one-shot remains consumed

        var repeat = new PriceAlertRule("diamond", "BUY_ABOVE", 100);
        repeat.repeat = true; repeat.cooldownSeconds = 10; repeat.rearmOnExit = false;
        assertEquals(1, engine.evaluate(List.of(repeat), snapshot, true, 1_000).size());
        assertEquals(0, engine.evaluate(List.of(repeat), snapshot, true, 10_999).size());
        assertEquals(1, engine.evaluate(List.of(repeat), snapshot, true, 11_000).size());

        repeat.rearmOnExit = true;
        assertEquals(0, engine.evaluate(List.of(repeat), Map.of("diamond", price(90, 80)), true, 12_000).size());
        assertEquals(0, engine.evaluate(List.of(repeat), snapshot, true, 13_000).size()); // cooldown still applies
        assertEquals(1, engine.evaluate(List.of(repeat), snapshot, true, 22_000).size());
        assertEquals(0, engine.evaluate(List.of(repeat), snapshot, false, 40_000).size());
    }
}
