package systems.diath.visotaris_opmod.util;

/**
 * Expands amount shortforms used in OPSUCHT inputs.
 *
 * Examples:
 *   1k -> 1000
 *   1.5m -> 1500000
 *   1,5m -> 1500000
 */
public final class AmountShortformExpander {
    private AmountShortformExpander() {
    }

    public static String expandAmount(String digits, String suffix) {
        // Punkt ist Tausendertrenner nur wenn auch ein Komma vorhanden ist.
        // Kein Komma -> Punkt ist Dezimalzeichen, z. B. "1.5k".
        String normalized = digits.contains(",")
                ? digits.replace(".", "").replace(",", ".")
                : digits;

        double number;
        try {
            number = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }

        double multiplier = switch (suffix.toLowerCase()) {
            case "k" -> 1_000.0;
            case "m" -> 1_000_000.0;
            case "b" -> 1_000_000_000.0;
            default -> 0.0;
        };

        if (multiplier == 0.0) return null;
        return Long.toString((long) (number * multiplier));
    }
}
