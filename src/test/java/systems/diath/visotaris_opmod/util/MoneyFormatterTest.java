package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyFormatterTest {
    @Test void prefixesNormalMoneyWithDollarAndUsesGermanFormatting() {
        assertEquals("$1.250.000,00", MoneyFormatter.format(1_250_000));
        assertEquals("$50.000,00", MoneyFormatter.format(50_000));
    }
}
