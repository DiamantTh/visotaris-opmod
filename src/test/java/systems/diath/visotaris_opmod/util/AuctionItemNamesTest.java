package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.AuctionItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionItemNamesTest {
    @Test void apiNameTakesPriorityAndDoesNotNeedCarrierLocalization() {
        AuctionItem item = new AuctionItem("PAPER", null, 1, "Gräbergemisch", null, null);
        assertEquals("Gräbergemisch", AuctionItemNames.visibleName(item, material -> {
            throw new AssertionError("an API display name must take priority");
        }));
    }

    @Test void usesLocalizedVanillaNameThenMaterialThenUnknownFallback() {
        assertEquals("Papier", AuctionItemNames.visibleName(new AuctionItem("PAPER", null, 1, null, null, null),
            material -> "Papier"));
        assertEquals("odd_material", AuctionItemNames.visibleName(new AuctionItem("odd_material", null, 1, null, null, null),
            material -> null));
        assertEquals("Unbekanntes Item", AuctionItemNames.visibleName(null, material -> null));
    }
}
