package systems.diath.visotaris.hooks;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests für AnvilScreenMixin.expandAmount (package-private).
 *
 * Überprüft insbesondere den Fix für den Dezimalpunkt-Bug:
 * "1.5k" muss 1500 ergeben, nicht 15000.
 */
class AnvilScreenMixinTest {

    // Semikolon als Trennzeichen, damit Komma in Dezimalzahlen (z.B. "1,5") kein Problem ist.
    @ParameterizedTest(name = "expandAmount(\"{0}\", \"{1}\") = \"{2}\"")
    @CsvSource(delimiterString = ";", value = {
        "1   ; k ; 1000",
        "1.5 ; k ; 1500",    // int'l Dezimal: 1.5 * 1000 = 1500  (Bug-Fix: war 15000)
        "1,5 ; k ; 1500",    // dt. Dezimal: Komma → 1.5 * 1000 = 1500
        "500 ; k ; 500000",
        "1.000; k; 1000",    // Kein Komma → Punkt als Dezimal → 1.0 * 1000 = 1000
        "2   ; m ; 2000000",
        "1.5 ; m ; 1500000",
        "2   ; b ; 2000000000",
        "1.5 ; B ; 1500000000",  // Suffix case-insensitive
        "100 ; K ; 100000",
    })
    void korrekteExpansion(String digits, String suffix, String expected) {
        assertEquals(expected.strip(), AnvilScreenMixin.expandAmount(digits.strip(), suffix.strip()));
    }

    @ParameterizedTest(name = "expandAmount(\"{0}\", \"{1}\") = null")
    @CsvSource(delimiterString = ";", value = {
        "1   ; x",   // unbekannter Suffix
        "abc ; k",   // keine Zahl
    })
    void ungueltigeEingabeGibtNull(String digits, String suffix) {
        assertNull(AnvilScreenMixin.expandAmount(digits.strip(), suffix.strip()));
    }
}
