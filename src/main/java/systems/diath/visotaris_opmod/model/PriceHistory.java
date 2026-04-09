package systems.diath.visotaris_opmod.model;

import java.util.Collections;
import java.util.List;

/**
 * Vollständige Preisverlauf-Antwort der OPSUCHT-API für ein Material.
 * Enthält vier Granularitätsstufen, die direkt von der API geliefert werden.
 */
public final class PriceHistory {

    public final List<PriceHistoryPoint> HOURLY;
    public final List<PriceHistoryPoint> DAILY;
    public final List<PriceHistoryPoint> WEEKLY;
    public final List<PriceHistoryPoint> MONTHLY;

    public PriceHistory(List<PriceHistoryPoint> hourly,
                        List<PriceHistoryPoint> daily,
                        List<PriceHistoryPoint> weekly,
                        List<PriceHistoryPoint> monthly) {
        this.HOURLY  = hourly;
        this.DAILY   = daily;
        this.WEEKLY  = weekly;
        this.MONTHLY = monthly;
    }

    public static PriceHistory empty() {
        return new PriceHistory(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }
}
