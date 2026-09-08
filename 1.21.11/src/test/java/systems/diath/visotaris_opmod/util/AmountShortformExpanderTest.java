package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmountShortformExpanderTest {

    @Test
    void expandsGermanAndInternationalShortforms() {
        assertEquals("1500", AmountShortformExpander.expandAmount("1.5", "k"));
        assertEquals("1500000", AmountShortformExpander.expandAmount("1,5", "m"));
        assertEquals("1234000", AmountShortformExpander.expandAmount("1.234", "m"));
    }

    @Test
    void rejectsUnknownSuffixes() {
        assertNull(AmountShortformExpander.expandAmount("4", "x"));
    }
}
