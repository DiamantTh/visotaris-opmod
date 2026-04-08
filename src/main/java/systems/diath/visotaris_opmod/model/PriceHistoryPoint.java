package systems.diath.visotaris_opmod.model;

/**
 * Ein Datenpunkt aus der OPSUCHT-Preisverlauf-API.
 *
 * Beispiel-API-Antwort {@code GET /market/history/{material}}:
 * <pre>
 * [
 *   {"timestamp": 1750000000000, "buyPrice": 49.5, "sellPrice": 3.8},
 *   ...
 * ]
 * </pre>
 */
public final class PriceHistoryPoint {

    public final long   timestamp;
    public final double buyPrice;
    public final double sellPrice;

    public PriceHistoryPoint(long timestamp, double buyPrice, double sellPrice) {
        this.timestamp = timestamp;
        this.buyPrice  = buyPrice;
        this.sellPrice = sellPrice;
    }
}
