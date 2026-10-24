package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSellerNamesTest {
    @Test void missingSellerIdIsExplicitlyAnonymous() {
        assertTrue(AuctionSellerNames.isAnonymous(null));
        assertTrue(AuctionSellerNames.isAnonymous("  "));
        assertEquals("Anonymer Verkäufer", AuctionSellerNames.visibleName(null, "CachedPlayer"));
    }

    @Test void suppliedSellerIdUsesOnlyItsResolvedProfileName() {
        assertFalse(AuctionSellerNames.isAnonymous("123e4567-e89b-12d3-a456-426614174000"));
        assertEquals("Spielername nicht verfügbar", AuctionSellerNames.visibleName("id", null));
        assertEquals("OPShadowWolf", AuctionSellerNames.visibleName("id", " OPShadowWolf "));
        assertEquals("Spielername wird aufgelöst", AuctionSellerNames.visibleName("id", null, true));
    }
}
