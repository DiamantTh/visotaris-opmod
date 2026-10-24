package systems.diath.visotaris_opmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemNameResolverTest {
    @Test void prefersBundledGermanVanillaNameOverTheActiveClientLanguage() {
        assertEquals("Schaf-Spawn-Ei", ItemNameResolver.resolve("SHEEP_SPAWN_EGG",
            key -> key.equals("item.minecraft.sheep_spawn_egg") ? "Schaf-Spawn-Ei" : null,
            key -> key.equals("item.minecraft.sheep_spawn_egg") ? "Sheep Spawn Egg" : null));
    }

    @Test void preservesCompoundSuffixAfterGermanVanillaResolution() {
        assertEquals("Papier (626)", ItemNameResolver.resolve("paper#626",
            key -> key.equals("item.minecraft.paper") ? "Papier" : null,
            key -> null));
    }
}
