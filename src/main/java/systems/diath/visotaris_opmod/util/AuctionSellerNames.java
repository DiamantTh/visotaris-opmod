package systems.diath.visotaris_opmod.util;

/** Presentation rule for seller identities supplied by the public auction API. */
public final class AuctionSellerNames {
    /** OPSUCHT's explicit seller-privacy sentinel in /auctions/active. */
    public static final String ANONYMOUS_SELLER_UUID = "00000000-0000-0000-0000-000000000000";
    public static final String ANONYMOUS_SELLER = "Anonymer Verkäufer";
    public static final String UNRESOLVED_SELLER = "Spielername nicht verfügbar";
    public static final String RESOLVING_SELLER = "Spielername wird aufgelöst";

    private AuctionSellerNames() { }

    /**
     * OPSUCHT keeps {@code seller} present. The all-zero UUID denotes an anonymous
     * seller and must never trigger a lookup or be inferred from other state.
     */
    public static boolean isAnonymous(String seller) {
        return seller == null || seller.isBlank() || ANONYMOUS_SELLER_UUID.equalsIgnoreCase(seller.trim());
    }

    public static String visibleName(String seller, String resolvedName) {
        return visibleName(seller, resolvedName, false);
    }

    public static String visibleName(String seller, String resolvedName, boolean resolving) {
        if (isAnonymous(seller)) return ANONYMOUS_SELLER;
        if (resolving) return RESOLVING_SELLER;
        return resolvedName == null || resolvedName.isBlank() ? UNRESOLVED_SELLER : resolvedName.trim();
    }
}
