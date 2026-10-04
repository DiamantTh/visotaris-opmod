package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSearchTest {
    private static final String UID = "107840b1b5eff52ddda93ae07eeaf714";
    private static final String SELLER_UUID = "de27aa9bfd33440dfac2c6eee9bc371d";

    private Auction auction() {
        return new Auction(UID, SELLER_UUID, new AuctionItem("GOLDEN_HORSE_ARMOR", null, 1,
            "Plüsch thalie", null, null), "custom_items", "ACTIVE", 1, null, 1,
            null, Map.of(), null, null);
    }

    @Test void localSearchMatchesVisibleItemNameAndAlreadyResolvedSeller() {
        Auction auction = auction();
        assertTrue(AuctionSearch.matches(auction, "DiamantTh", "Plüsch thalie", "plü"));
        assertTrue(AuctionSearch.matches(auction, "DiamantTh", "Plüsch thalie", "dia"));
        assertFalse(AuctionSearch.matches(auction, null, "Plüsch thalie", "dia"));
    }

    @Test void technicalCarrierAndPrivateIdentifiersAreNotSearchFieldsForCustomItems() {
        Auction auction = auction();
        String fields = AuctionSearch.searchableText(auction, "DiamantTh", "Plüsch thalie");
        assertFalse(fields.contains("golden_horse_armor"));
        assertFalse(fields.contains(UID));
        assertFalse(fields.contains(SELLER_UUID));
        assertFalse(AuctionSearch.matches(auction, "DiamantTh", "Plüsch thalie", "de27aa9b"));
    }
}
