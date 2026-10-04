package systems.diath.visotaris_opmod.util;

import systems.diath.visotaris_opmod.model.Auction;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.stream.Collectors;

/** Local auction search fields shared by the Minecraft and Web UIs. IDs are intentionally excluded. */
public final class AuctionSearch {
    private AuctionSearch() { }

    public static String searchableText(Auction auction, String sellerName, String visibleItemName) {
        if (auction == null) return "";
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        add(fields, visibleItemName);
        // The API material may only be a technical carrier for a named custom item.
        // Include it as an alias only when no API display name was supplied.
        if (auction.item() != null && (auction.item().displayName() == null || auction.item().displayName().isBlank()))
            add(fields, auction.item().material());
        add(fields, sellerName);
        return fields.stream().map(AuctionSearch::normalize).filter(value -> !value.isEmpty())
            .collect(Collectors.joining(" "));
    }

    public static boolean matches(Auction auction, String sellerName, String visibleItemName, String query) {
        String normalizedQuery = normalize(query);
        return normalizedQuery.isEmpty() || searchableText(auction, sellerName, visibleItemName).contains(normalizedQuery);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void add(LinkedHashSet<String> fields, String value) {
        if (value != null && !value.isBlank()) fields.add(value.trim());
    }
}
