package systems.diath.visotaris_opmod.model;

/**
 * Unveränderliches Datenobjekt eines Marktpreis-Eintrags.
 * Quelle: https://api.opsucht.net/market/prices
 *
 * TODO: Tatsächlichen Feldnamen der API verifizieren und ggf. anpassen.
 */
public final class MarketPrice {

    private final String itemKey;
    private final double buy;
    private final double sell;

    public MarketPrice(String itemKey, double buy, double sell) {
        this.itemKey = itemKey;
        this.buy     = buy;
        this.sell    = sell;
    }

    public String getItemKey() { return itemKey; }
    public double getBuy()     { return buy; }
    public double getSell()    { return sell; }

    @Override
    public String toString() {
        return "MarketPrice{itemKey='" + itemKey + "', buy=" + buy + ", sell=" + sell + '}';
    }
}
