package systems.diath.visotaris_opmod.model;

/**
 * Ergebnis der Bewertung eines Inventars oder Containers.
 * Wird von {@code InventoryValuationService} berechnet und
 * von HUD, Screen-Overlay und Tooltip-Schicht genutzt.
 */
public final class InventoryValuation {

    private final double buyTotal;
    private final double sellTotal;
    private final double shardTotal;
    private final double redcoinTotal;
    private final boolean hasShards;
    private final boolean hasRedcoins;
    private final boolean hasShulkers;

    public InventoryValuation(double buyTotal, double sellTotal,
                              double shardTotal, boolean hasShards, boolean hasShulkers) {
        this(buyTotal, sellTotal, shardTotal, 0, hasShards, false, hasShulkers);
    }

    public InventoryValuation(double buyTotal, double sellTotal, double shardTotal,
                              double redcoinTotal, boolean hasShards, boolean hasRedcoins,
                              boolean hasShulkers) {
        this.buyTotal    = buyTotal;
        this.sellTotal   = sellTotal;
        this.shardTotal  = shardTotal;
        this.redcoinTotal = redcoinTotal;
        this.hasShards   = hasShards;
        this.hasRedcoins = hasRedcoins;
        this.hasShulkers = hasShulkers;
    }

    public static InventoryValuation empty() {
        return new InventoryValuation(0, 0, 0, 0, false, false, false);
    }

    public double getBuyTotal()   { return buyTotal; }
    public double getSellTotal()  { return sellTotal; }
    public double getShardTotal() { return shardTotal; }
    public double getRedcoinTotal() { return redcoinTotal; }
    public boolean hasShards()    { return hasShards; }
    public boolean hasRedcoins()  { return hasRedcoins; }
    public boolean hasShulkers()  { return hasShulkers; }
}
