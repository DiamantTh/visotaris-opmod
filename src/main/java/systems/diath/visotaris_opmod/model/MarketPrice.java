package systems.diath.visotaris_opmod.model;

/**
 * Unveränderliches Datenobjekt eines Marktpreis-Eintrags.
 * Quelle: https://api.opsucht.net/market/prices
 *
 * <p>Die API liefert pro Item separate BUY-/SELL-Einträge mit
 * {@code price} und {@code activeOrders}; Root ist Kategorie → Items.
 */
public final class MarketPrice {

    private final String itemKey;
    private final double buy;
    private final double sell;
    private final int    buyOrders;
    private final int    sellOrders;
    private final String category;

    public MarketPrice(String itemKey, double buy, double sell,
                       int buyOrders, int sellOrders, String category) {
        this.itemKey    = itemKey;
        this.buy        = buy;
        this.sell       = sell;
        this.buyOrders  = buyOrders;
        this.sellOrders = sellOrders;
        this.category   = category;
    }

    /** Kompatibilitäts-Konstruktor (alte Disk-Caches). */
    public MarketPrice(String itemKey, double buy, double sell) {
        this(itemKey, buy, sell, 0, 0, null);
    }

    public String getItemKey()    { return itemKey; }
    public double getBuy()        { return buy; }
    public double getSell()       { return sell; }
    public int    getBuyOrders()  { return buyOrders; }
    public int    getSellOrders() { return sellOrders; }
    public String getCategory()   { return category; }

    @Override
    public String toString() {
        return "MarketPrice{itemKey='" + itemKey + "', buy=" + buy + ", sell=" + sell
            + ", buyOrders=" + buyOrders + ", sellOrders=" + sellOrders
            + ", category='" + category + "'}";
    }
}
