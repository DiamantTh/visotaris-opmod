package systems.diath.visotaris_opmod.util;

import systems.diath.visotaris_opmod.model.Auction;

import java.util.function.Function;

/**
 * Resolves names sent to the local Web UX. Auction names share the same
 * display-name priority and vanilla-alias rules as the native auction UX.
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
        return auctionName(auction, AuctionItemNames::localizedVanillaName);
    }

    static String auctionName(Auction auction, Function<String, String> materialLocalizer) {
        return AuctionItemNames.visibleName(auction == null ? null : auction.item(), materialLocalizer);
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

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
