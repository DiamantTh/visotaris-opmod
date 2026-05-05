package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for AmountShortformExpander.
 *
 * Covers the decimal point fix: "1.5k" must become 1500, not 15000.
 */
class AmountShortformExpanderTest {

    // Semicolon delimiter keeps decimal commas such as "1,5" easy to read.
    @ParameterizedTest(name = "expandAmount(\"{0}\", \"{1}\") = \"{2}\"")
    @CsvSource(delimiterString = ";", value = {
        "1   ; k ; 1000",
        "1.5 ; k ; 1500",
        "1,5 ; k ; 1500",
        "500 ; k ; 500000",
        "1.000; k; 1000",
        "2   ; m ; 2000000",
        "1.5 ; m ; 1500000",
        "2   ; b ; 2000000000",
        "1.5 ; B ; 1500000000",
        "100 ; K ; 100000",
    })
    void correctExpansion(String digits, String suffix, String expected) {
        assertEquals(expected.strip(), AmountShortformExpander.expandAmount(digits.strip(), suffix.strip()));
    }

    @ParameterizedTest(name = "expandAmount(\"{0}\", \"{1}\") = null")
    @CsvSource(delimiterString = ";", value = {
        "1   ; x",
        "abc ; k",
    })
    void invalidInputReturnsNull(String digits, String suffix) {
        assertNull(AmountShortformExpander.expandAmount(digits.strip(), suffix.strip()));
    }
}
