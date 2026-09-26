package systems.diath.visotaris_opmod.services;

import systems.diath.visotaris_opmod.config.PriceAlertRule;
import systems.diath.visotaris_opmod.model.MarketPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure in-memory alert evaluation; it never accesses a network or Minecraft server. */
public final class PriceAlertEngine {
    public static final Set<String> CONDITIONS = Set.of("BUY_ABOVE", "BUY_BELOW", "SELL_ABOVE", "SELL_BELOW", "SPREAD_ABOVE", "SPREAD_BELOW");

    public record Fired(PriceAlertRule rule, double currentValue, long timestampMs) { }

    public List<Fired> evaluate(List<PriceAlertRule> rules, Map<String, MarketPrice> prices, boolean globallyEnabled, long nowMs) {
        List<Fired> fired = new ArrayList<>();
        if (!globallyEnabled) return fired;
        for (PriceAlertRule rule : rules) {
            if (!rule.enabled || !CONDITIONS.contains(rule.condition)) continue;
            MarketPrice price = prices.get(rule.itemKey.toLowerCase());
            if (price == null) continue;
            double value = value(rule.condition, price);
            boolean matches = matches(rule.condition, value, rule.threshold);
            if (!matches) {
                if (rule.rearmOnExit) rule.latched = false;
                continue;
            }
            if (rule.rearmOnExit && rule.latched) continue;
            if (!rule.repeat && rule.triggered) continue;
            if (rule.repeat && rule.lastTriggeredAtMs > 0 && nowMs - rule.lastTriggeredAtMs < rule.cooldownSeconds * 1000L) continue;
            rule.lastTriggeredAtMs = nowMs;
            rule.triggered = true;
            rule.latched = true;
            fired.add(new Fired(rule, value, nowMs));
        }
        return fired;
    }

    public static double value(String condition, MarketPrice p) {
        return switch (condition) {
            case "BUY_ABOVE", "BUY_BELOW" -> p.getBuy() > 0 ? p.getBuy() : Double.NaN;
            case "SELL_ABOVE", "SELL_BELOW" -> p.getSell() > 0 ? p.getSell() : Double.NaN;
            case "SPREAD_ABOVE", "SPREAD_BELOW" -> p.getBuy() > 0 && p.getSell() > 0 ? p.getBuy() - p.getSell() : Double.NaN;
            default -> Double.NaN;
        };
    }

    public static boolean matches(String condition, double value, double threshold) {
        if (!Double.isFinite(value) || !Double.isFinite(threshold)) return false;
        return switch (condition) {
            case "BUY_ABOVE", "SELL_ABOVE", "SPREAD_ABOVE" -> value > threshold;
            case "BUY_BELOW", "SELL_BELOW", "SPREAD_BELOW" -> value < threshold;
            default -> false;
        };
    }
}
