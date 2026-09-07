package systems.diath.visotaris_opmod.model;

import java.util.Locale;

/** Zielwährungen der OPSUCHT-Merchant-API. */
public enum MerchantCurrency {
    OPSHARDS("opshards", "OPS", "Shardkurs"),
    REDCOINS("redcoins", "Redcoins", "Redcoin-Kurs"),
    UNKNOWN("", "", "Händlerkurs");

    private final String apiTarget;
    private final String displayUnit;
    private final String displayLabel;

    MerchantCurrency(String apiTarget, String displayUnit, String displayLabel) {
        this.apiTarget = apiTarget;
        this.displayUnit = displayUnit;
        this.displayLabel = displayLabel;
    }

    public String getDisplayUnit() { return displayUnit; }
    public String getDisplayLabel() { return displayLabel; }

    public static MerchantCurrency fromApiTarget(String target) {
        if (target == null) return UNKNOWN;
        String normalized = target.trim().toLowerCase(Locale.ROOT);
        for (MerchantCurrency currency : values()) {
            if (currency.apiTarget.equals(normalized)) return currency;
        }
        return UNKNOWN;
    }
}
