package systems.diath.visotaris_opmod.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantCurrencyTest {
    @Test void usesFullDisplayUnitsForKnownMerchantTargets() {
        assertEquals("OPShards", MerchantCurrency.fromApiTarget("opshards").getDisplayUnit());
        assertEquals("Redcoins", MerchantCurrency.fromApiTarget("redcoins").getDisplayUnit());
    }

    @Test void keepsUnknownTargetsAsReadableFallbacks() {
        assertEquals(MerchantCurrency.UNKNOWN, MerchantCurrency.fromApiTarget("future_coins"));
        assertEquals("future coins", MerchantCurrency.displayUnitFor("future_coins"));
    }
}
