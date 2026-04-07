package systems.diath.visotaris.model;

/**
 * Ergebnis der Bewertung eines Inventars oder Containers.
 * Wird von {@code InventoryValuationService} berechnet und
 * von HUD, Screen-Overlay und Tooltip-Schicht genutzt.
 */
public final class InventoryValuation {

    private final double buyTotal;
    private final double sellTotal;
    private final double shardTotal;
    private final boolean hasShards;
    private final boolean hasShulkers;

    public InventoryValuation(double buyTotal, double sellTotal,
                              double shardTotal, boolean hasShards, boolean hasShulkers) {
        this.buyTotal    = buyTotal;
        this.sellTotal   = sellTotal;
        this.shardTotal  = shardTotal;
        this.hasShards   = hasShards;
        this.hasShulkers = hasShulkers;
    }

    public static InventoryValuation empty() {
        return new InventoryValuation(0, 0, 0, false, false);
    }

    public double getBuyTotal()   { return buyTotal; }
    public double getSellTotal()  { return sellTotal; }
    public double getShardTotal() { return shardTotal; }
    public boolean hasShards()    { return hasShards; }
    public boolean hasShulkers()  { return hasShulkers; }
}
