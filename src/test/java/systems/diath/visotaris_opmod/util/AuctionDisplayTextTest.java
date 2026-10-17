package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuctionDisplayTextTest {
    @Test
    void formatsLocalizedEnchantmentWithMinecraftLevel() {
        Map<String, String> translations = Map.of(
            "enchantment.minecraft.sharpness", "Schärfe",
            "enchantment.level.5", "V"
        );

        assertEquals("Schärfe V", AuctionDisplayText.enchantment("minecraft:sharpness", 5, translations::get));
    }

    @Test
    void usesGermanVanillaFallbackAndReadableUnknownFallback() {
        assertEquals("Haltbarkeit III", AuctionDisplayText.enchantment("minecraft:unbreaking", 3, ignored -> null));

        String unknown = AuctionDisplayText.enchantment("example:keen_edge", 2, ignored -> null);
        assertEquals("Unbekannte Verzauberung (keen edge) II", unknown);
        assertFalse(unknown.contains("example:"));
    }

    @Test
    void translatesAuctionCategoryAndStatusKeys() {
        assertEquals("Benutzerdefinierte Items", AuctionDisplayText.category("custom_items"));
        assertEquals("Sofort gekauft", AuctionDisplayText.status("INSTANT_BOUGHT"));
    }
}
