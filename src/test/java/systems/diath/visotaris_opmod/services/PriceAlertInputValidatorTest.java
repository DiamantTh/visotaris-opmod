package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceAlertInputValidatorTest {
    @Test void allowsOnlyKnownFieldsAndBounds() {
        assertEquals("HUD", PriceAlertInputValidator.create("paper#626", "buy_above", 250, true, false, 300, true, "chat").notification);
        assertEquals("HUD_WEB", PriceAlertInputValidator.create("diamond", "BUY_ABOVE", 250, true, false, 300, true, "BOTH").notification);
        assertEquals("HUD_WEB", PriceAlertInputValidator.create("diamond", "BUY_ABOVE", 250, true, false, 300, true, "HUD_WEB").notification);
        assertNull(PriceAlertInputValidator.create("../system", "BUY_ABOVE", 1, true, false, 300, true, "CHAT"));
        assertNull(PriceAlertInputValidator.create("diamond", "RUN_COMMAND", 1, true, false, 300, true, "CHAT"));
        assertNull(PriceAlertInputValidator.create("diamond", "BUY_ABOVE", Double.NaN, true, false, 300, true, "CHAT"));
        assertNull(PriceAlertInputValidator.create("diamond", "BUY_ABOVE", 1, true, true, 9, true, "CHAT"));
        assertNull(PriceAlertInputValidator.create("diamond", "BUY_ABOVE", 1, true, false, 300, true, "WEBHOOK"));
    }
}
