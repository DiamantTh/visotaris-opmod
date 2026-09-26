package systems.diath.visotaris_opmod.services;

import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.config.PriceAlertRule;
import systems.diath.visotaris_opmod.model.MarketPrice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Evaluates rules only after a successful MarketCache update. */
public final class PriceAlertService {
    public record Event(String id, String itemKey, String condition, double threshold, double currentValue, long timestampMs, String notification) { }
    private final ConfigManager config;
    private final Consumer<Event> hudNotifier;
    private final PriceAlertEngine engine = new PriceAlertEngine();
    private final Deque<Event> events = new ArrayDeque<>();

    public PriceAlertService(ConfigManager config, MarketCache cache, Consumer<Event> hudNotifier) {
        this.config = config;
        this.hudNotifier = hudNotifier;
        cache.addUpdateListener(this::evaluate);
    }

    private void evaluate(Map<String, MarketPrice> snapshot) {
        List<PriceAlertEngine.Fired> fired;
        synchronized (config) {
            var cfg = config.getConfig();
            fired = engine.evaluate(cfg.priceAlertRules, snapshot, cfg.priceAlertsEnabled, System.currentTimeMillis());
            if (!fired.isEmpty()) config.save();
        }
        for (var alarm : fired) {
            String channel = alarm.rule().notificationChannel();
            Event event = new Event(alarm.rule().id, alarm.rule().itemKey, alarm.rule().condition,
                alarm.rule().threshold, alarm.currentValue(), alarm.timestampMs(), channel);
            synchronized (events) {
                events.addFirst(event);
                while (events.size() > 50) events.removeLast();
            }
            if (channel.equals("HUD") || channel.equals("HUD_WEB")) hudNotifier.accept(event);
        }
    }

    public static String conditionLabel(String condition) {
        return switch (condition) {
            case "BUY_ABOVE" -> "Kaufpreis über";
            case "BUY_BELOW" -> "Kaufpreis unter";
            case "SELL_ABOVE" -> "Verkaufspreis über";
            case "SELL_BELOW" -> "Verkaufspreis unter";
            case "SPREAD_ABOVE" -> "Spanne über";
            case "SPREAD_BELOW" -> "Spanne unter";
            default -> condition;
        };
    }

    public List<Event> events() { synchronized (events) { return new ArrayList<>(events); } }
}
