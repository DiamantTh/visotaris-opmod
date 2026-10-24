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

    @Test void customDisplayNamesRemainVisibleOnPaperCarriers() {
        for (String displayName : new String[]{"Spawnersplitter", "+10% Speed Upgrade Modul"}) {
            AuctionItem item = new AuctionItem("PAPER", null, 1, displayName, null, null);
            assertEquals(displayName, AuctionItemNames.visibleName(item, material -> {
                throw new AssertionError("the technical carrier must not replace an auction display name");
            }));
        }
    }

    @Test void usesLocalizedVanillaNameThenMaterialThenUnknownFallback() {
        assertEquals("Papier", AuctionItemNames.visibleName(new AuctionItem("PAPER", null, 1, null, null, null),
            material -> "Papier"));
        assertEquals("odd_material", AuctionItemNames.visibleName(new AuctionItem("odd_material", null, 1, null, null, null),
            material -> null));
        assertEquals("Unbekanntes Item", AuctionItemNames.visibleName(null, material -> null));
    }

    @Test void localizesApiNamesThatAreJustVanillaMaterialNames() {
        AuctionItem item = new AuctionItem("ANCIENT_DEBRIS", null, 1, "Ancient Debris", null, null);
        assertEquals("Antiker Schrott", AuctionItemNames.visibleName(item, material -> "Antiker Schrott"));
    }

    @Test void localizesDisplayNameAliasesOfVanillaMaterials() {
        AuctionItem item = new AuctionItem("DIAMOND", null, 1, "DIAMOND", null, null);
        assertEquals("Diamant", AuctionItemNames.visibleName(item, material ->
            material.equalsIgnoreCase("DIAMOND") ? "Diamant" : null));
    }

    @Test void localizesNamelessVanillaSpawnEggsInsteadOfShowingTheirRegistryKeys() {
        String[][] spawnEggs = {
            {"BREEZE_SPAWN_EGG", "Breeze-Spawn-Ei"},
            {"OCELOT_SPAWN_EGG", "Ozelot-Spawn-Ei"},
            {"SHEEP_SPAWN_EGG", "Schaf-Spawn-Ei"},
            {"ZOMBIE_SPAWN_EGG", "Zombie-Spawn-Ei"}
        };

        for (String[] spawnEgg : spawnEggs) {
            AuctionItem item = new AuctionItem(spawnEgg[0], null, 1, null, null, null);
            assertEquals(spawnEgg[1], AuctionItemNames.visibleName(item, material ->
                material.equalsIgnoreCase(spawnEgg[0]) ? spawnEgg[1] : null));
        }
    }
}
