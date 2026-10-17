package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebItemNameResolverTest {
    @Test void vanillaLocalizationWinsOverAnEnglishApiName() {
        assertEquals("Erde", WebItemNameResolver.vanillaOrFallback("DIRT", "Dirt", material -> "Erde"));
    }

    @Test void auctionDisplayNameWinsRegardlessOfCategoryIconOrCarrier() {
        for (String displayName : new String[]{"Spawnersplitter", "+10% Speed Upgrade Modul"}) {
            Auction auction = new Auction("uid", "seller",
                new AuctionItem("PAPER", null, 1, displayName, null, null),
                "misc", "ACTIVE", 0, null, 0, null, null, null, null);
            assertEquals(displayName, WebItemNameResolver.auctionName(auction, material -> {
                throw new AssertionError("the technical carrier must not replace an auction display name");
            }));
        }
    }

    @Test void auctionNameUsesTheSameVanillaAliasRuleAsNativeUi() {
        AuctionItem item = new AuctionItem("DIAMOND", null, 1, "DIAMOND", null, null);
        Auction auction = new Auction("uid", null, item, "weapons", "ACTIVE", 0,
            null, 0, null, null, null, null);
        var materialLocalizer = (java.util.function.Function<String, String>) material ->
            material.equalsIgnoreCase("DIAMOND") ? "Diamant" : null;

        assertEquals(AuctionItemNames.visibleName(item, materialLocalizer),
            WebItemNameResolver.auctionName(auction, materialLocalizer));
        assertEquals("Diamant", WebItemNameResolver.auctionName(auction, materialLocalizer));
        assertEquals("Unbekanntes Item", WebItemNameResolver.auctionName(null, materialLocalizer));
    }

    @Test void unresolvedMaterialUsesApiNameThenMaterialThenUnknown() {
        assertEquals("API Name", WebItemNameResolver.vanillaOrFallback("unknown_material", "API Name", ignored -> null));
        assertEquals("unknown_material", WebItemNameResolver.vanillaOrFallback("unknown_material", null, ignored -> null));
        assertEquals("Unbekanntes Item", WebItemNameResolver.vanillaOrFallback(null, null, ignored -> null));
    }
}
