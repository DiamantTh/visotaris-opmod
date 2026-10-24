package systems.diath.visotaris_opmod.util;

/** Presentation rule for seller identities supplied by the public auction API. */
public final class AuctionSellerNames {
    public static final String ANONYMOUS_SELLER = "Anonymer Verkäufer";
    public static final String UNRESOLVED_SELLER = "Spielername nicht verfügbar";

    private AuctionSellerNames() { }

    /**
     * A missing seller identifier is an explicit anonymous auction. It must never
     * trigger a lookup or be inferred from unrelated client state.
     */
    public static boolean isAnonymous(String seller) {
        return seller == null || seller.isBlank();
    }

    public static String visibleName(String seller, String resolvedName) {
        if (isAnonymous(seller)) return ANONYMOUS_SELLER;
        return resolvedName == null || resolvedName.isBlank() ? UNRESOLVED_SELLER : resolvedName.trim();
    }
}
