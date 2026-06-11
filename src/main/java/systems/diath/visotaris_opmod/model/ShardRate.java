package systems.diath.visotaris_opmod.model;

/**
 * Unveränderliches Datenobjekt eines Merchant-/Shardkurs-Eintrags.
 * Quelle: https://api.opsucht.net/merchant/rates
 *
 * <p>Die API liefert {@code source}, {@code target} (z.B. "opshards"),
 * {@code base} (Basiskurs) und {@code exchangeRate} (aktueller Kurs).
 * Das Feld {@code source} wird vor der Ablage im Cache normalisiert
 * (via {@code MerchantApiClient.normalizeSource}). Bei Custom-Items
 * (Paper mit custom_name) enthält {@code displayName} den lesbaren Namen.
 */
public final class ShardRate {

    private final String source;
    private final double exchangeRate;
    private final double base;
    private final String target;
    private final String displayName;

    public ShardRate(String source, double exchangeRate,
                     double base, String target, String displayName) {
        this.source       = source;
        this.exchangeRate = exchangeRate;
        this.base         = base;
        this.target       = target;
        this.displayName  = displayName;
    }

    /** Kompatibilitäts-Konstruktor (alte Disk-Caches). */
    public ShardRate(String source, double exchangeRate) {
        this(source, exchangeRate, 0.0, null, null);
    }

    public String getSource()       { return source; }
    public double getExchangeRate() { return exchangeRate; }
    public double getBase()         { return base; }
    public String getTarget()       { return target; }
    public String getDisplayName()  { return displayName; }

    @Override
    public String toString() {
        return "ShardRate{source='" + source + "', exchangeRate=" + exchangeRate
            + ", base=" + base + ", target='" + target
            + "', displayName='" + displayName + "'}";
    }
}
