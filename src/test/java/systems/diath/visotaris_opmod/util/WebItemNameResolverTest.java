package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebItemNameResolverTest {
    @Test void vanillaLocalizationWinsOverAnEnglishApiName() {
        assertEquals("Erde", WebItemNameResolver.vanillaOrFallback("DIRT", "Dirt", material -> "Erde"));
    }

    @Test void customAuctionKeepsItsApiNameInsteadOfTheCarrierName() {
        Auction auction = new Auction("uid", null,
            new AuctionItem("PAPER", "https://items.opsucht.net/item.png", 1, "Boosterpack", null, null),
            "custom_items", "ACTIVE", 0, null, 0, null, null, null, null);
        assertEquals("Boosterpack", WebItemNameResolver.auctionName(auction));
    }

    @Test void unresolvedMaterialUsesApiNameThenMaterialThenUnknown() {
        assertEquals("API Name", WebItemNameResolver.vanillaOrFallback("unknown_material", "API Name", ignored -> null));
        assertEquals("unknown_material", WebItemNameResolver.vanillaOrFallback("unknown_material", null, ignored -> null));
        assertEquals("Unbekanntes Item", WebItemNameResolver.vanillaOrFallback(null, null, ignored -> null));
    }
}
