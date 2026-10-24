package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSellerNamesTest {
    @Test void opsuchtAllZeroSellerUuidIsExplicitlyAnonymous() {
        assertTrue(AuctionSellerNames.isAnonymous(null));
        assertTrue(AuctionSellerNames.isAnonymous("  "));
        assertTrue(AuctionSellerNames.isAnonymous(AuctionSellerNames.ANONYMOUS_SELLER_UUID));
        assertEquals("Anonymer Verkäufer", AuctionSellerNames.visibleName(AuctionSellerNames.ANONYMOUS_SELLER_UUID, "CachedPlayer"));
    }

    @Test void suppliedSellerIdUsesOnlyItsResolvedProfileName() {
        assertFalse(AuctionSellerNames.isAnonymous("123e4567-e89b-12d3-a456-426614174000"));
        assertEquals("Spielername nicht verfügbar", AuctionSellerNames.visibleName("id", null));
        assertEquals("OPShadowWolf", AuctionSellerNames.visibleName("id", " OPShadowWolf "));
        assertEquals("Spielername wird aufgelöst", AuctionSellerNames.visibleName("id", null, true));
    }
}
