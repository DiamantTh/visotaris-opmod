package systems.diath.visotaris_opmod.util;

import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import java.net.URI;
import java.util.Locale;
import java.util.function.Function;

/**
 * Resolves visible names sent to the local Web UX. Vanilla names come from the
 * running client's language resources; named OPSUCHT items keep their API name.
 */
public final class WebItemNameResolver {
    private static final String UNKNOWN_ITEM = "Unbekanntes Item";

    private WebItemNameResolver() { }

    public static String marketName(String itemKey) {
        return vanillaOrFallback(itemKey, null);
    }

    public static String merchantName(String source, String displayName) {
        return hasText(displayName) ? displayName.trim() : vanillaOrFallback(source, null);
    }

    public static String auctionName(Auction auction) {
        if (auction == null || auction.item() == null) return UNKNOWN_ITEM;
        AuctionItem item = auction.item();
        if (isCustomAuction(auction) && hasText(item.displayName())) return item.displayName().trim();
        return vanillaOrFallback(item.material(), item.displayName());
    }

    static String vanillaOrFallback(String material, String apiDisplayName) {
        return vanillaOrFallback(material, apiDisplayName, ItemNameResolver::resolveVanilla);
    }

    static String vanillaOrFallback(String material, String apiDisplayName, Function<String, String> localizer) {
        if (hasText(material)) {
            String localized = localizer.apply(material);
            if (hasText(localized)) return localized.trim();
        }
        if (hasText(apiDisplayName)) return apiDisplayName.trim();
        if (hasText(material)) return material.trim();
        return UNKNOWN_ITEM;
    }

    private static boolean isCustomAuction(Auction auction) {
        String category = auction.category() == null ? "" : auction.category().toLowerCase(Locale.ROOT);
        return category.startsWith("custom_") || category.startsWith("op_")
            || isOpsuchtIcon(auction.item().icon());
    }

    private static boolean isOpsuchtIcon(String rawUrl) {
        if (!hasText(rawUrl)) return false;
        try {
            String host = URI.create(rawUrl.trim()).getHost();
            return host != null && host.equalsIgnoreCase("items.opsucht.net");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
