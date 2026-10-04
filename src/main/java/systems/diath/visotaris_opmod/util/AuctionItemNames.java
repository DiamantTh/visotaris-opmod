package systems.diath.visotaris_opmod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import java.net.URI;
import java.util.Locale;
import java.util.function.Function;

/** Shared visible-name and safe carrier fallback rules for both auction UIs. */
public final class AuctionItemNames {
    private static final String UNKNOWN_ITEM = "Unbekanntes Item";

    private AuctionItemNames() { }

    public static String visibleName(AuctionItem item) {
        return visibleName(item, AuctionItemNames::localizedVanillaName);
    }

    static String visibleName(AuctionItem item, Function<String, String> materialLocalizer) {
        if (item == null) return UNKNOWN_ITEM;
        if (hasText(item.displayName())) return item.displayName().trim();
        if (hasText(item.material())) {
            String localized = materialLocalizer.apply(item.material());
            if (hasText(localized)) return localized.trim();
            return item.material().trim();
        }
        return UNKNOWN_ITEM;
    }

    /** Avoid showing PAPER/armor-carrier art while the API's custom icon is still loading. */
    public static String fallbackMaterial(Auction auction) {
        if (auction == null || auction.item() == null || !hasText(auction.item().material())) return "barrier";
        AuctionItem item = auction.item();
        String category = auction.category() == null ? "" : auction.category().toLowerCase(Locale.ROOT);
        if (category.startsWith("custom_") || category.startsWith("op_")) return "barrier";
        if (isOpsuchtIcon(item.icon())) return "barrier";

        String displayName = item.displayName();
        String vanillaName = localizedVanillaName(item.material());
        if (hasText(displayName) && hasText(vanillaName) && !displayName.trim().equalsIgnoreCase(vanillaName.trim()))
            return "barrier";
        return item.material();
    }

    private static String localizedVanillaName(String material) {
        if (!hasText(material)) return null;
        String raw = material.trim();
        Identifier id = Identifier.tryParse(raw);
        if (id == null) id = Identifier.tryParse("minecraft:" + raw.toLowerCase(Locale.ROOT));
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR && !"minecraft:air".equals(id.toString())) return null;
        String translationKey = item.getDescriptionId();
        String translated = Component.translatable(translationKey).getString();
        return translationKey.equals(translated) ? null : translated;
    }

    private static boolean isOpsuchtIcon(String rawUrl) {
        if (!hasText(rawUrl)) return false;
        try {
            String host = URI.create(rawUrl.trim()).getHost();
            return host != null && host.equalsIgnoreCase("items.opsucht.net");
        } catch (IllegalArgumentException ignored) { return false; }
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
