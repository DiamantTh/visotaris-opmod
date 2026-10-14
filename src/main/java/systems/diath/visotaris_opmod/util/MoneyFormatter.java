package systems.diath.visotaris_opmod.util;

import java.util.Locale;

/** Gemeinsame Darstellung der normalen OPSUCHT-Dollarwerte. */
public final class MoneyFormatter {
    private MoneyFormatter() { }

    public static String format(double value) {
        if (!Double.isFinite(value)) return "–";
        return "$" + String.format(Locale.GERMANY, "%,.2f", value);
    }

    public static String formatCompact(double value) {
        if (!Double.isFinite(value)) return "–";
        if (Math.abs(value) >= 1_000_000) return "$" + String.format(Locale.GERMANY, "%.1fM", value / 1_000_000);
        if (Math.abs(value) >= 1_000) return "$" + String.format(Locale.GERMANY, "%.1fK", value / 1_000);
        return "$" + String.format(Locale.GERMANY, "%.0f", value);
    }
}
