package systems.diath.visotaris.model;

/**
 * Unveränderliches Datenobjekt eines Merchant-/Shardkurs-Eintrags.
 * Quelle: https://api.opsucht.net/merchant/rates
 *
 * Das Feld {@code source} wird vor der Ablage im Cache normalisiert
 * (Leerzeichen, Groß-/Kleinschreibung), da der rohe API-Wert
 * möglicherweise nicht konsistent ist.
 *
 * TODO: Tatsächlichen Feldnamen der API verifizieren und ggf. anpassen.
 */
public final class ShardRate {

    private final String source;
    private final double exchangeRate;

    public ShardRate(String source, double exchangeRate) {
        this.source       = source;
        this.exchangeRate = exchangeRate;
    }

    public String getSource()       { return source; }
    public double getExchangeRate() { return exchangeRate; }

    @Override
    public String toString() {
        return "ShardRate{source='" + source + "', exchangeRate=" + exchangeRate + '}';
    }
}
