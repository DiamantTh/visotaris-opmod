package systems.diath.visotaris_opmod.model;

/**
 * Ein Datenpunkt aus der OPSUCHT-Preisverlauf-API.
 *
 * Beispiel-API-Antwort {@code GET /market/history/{material}}:
 * <pre>
 * {
 *   "HOURLY": [
 *     {"avgPrice":21.2,"minPrice":21.2,"maxPrice":21.2,"items":500,"transactions":3,"timestamp":"2026-03-25T23:00:00"},
 *     ...
 *   ],
 *   "DAILY":   [...],
 *   "WEEKLY":  [...],
 *   "MONTHLY": [...]
 * }
 * </pre>
 */
public final class PriceHistoryPoint {

    public final String timestamp;     // ISO-8601: "2026-03-25T23:00:00"
    public final double avgPrice;
    public final double minPrice;
    public final double maxPrice;
    public final int    items;
    public final int    transactions;

    public PriceHistoryPoint(String timestamp, double avgPrice, double minPrice,
                             double maxPrice, int items, int transactions) {
        this.timestamp    = timestamp;
        this.avgPrice     = avgPrice;
        this.minPrice     = minPrice;
        this.maxPrice     = maxPrice;
        this.items        = items;
        this.transactions = transactions;
    }
}
