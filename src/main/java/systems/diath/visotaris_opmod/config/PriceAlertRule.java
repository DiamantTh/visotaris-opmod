package systems.diath.visotaris_opmod.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persisted, observation-only market alert. No rule can perform a trade. */
public final class PriceAlertRule {
    public String id = UUID.randomUUID().toString();
    public String itemKey = "";
    public String condition = "BUY_ABOVE";
    public double threshold;
    public boolean enabled = true;
    public boolean repeat;
    public int cooldownSeconds = 300;
    public boolean rearmOnExit = true;
    public String notification = "CHAT";
    public long lastTriggeredAtMs;
    public boolean triggered;
    public boolean latched;

    public PriceAlertRule() { }

    public PriceAlertRule(String itemKey, String condition, double threshold) {
        this.itemKey = itemKey;
        this.condition = condition;
        this.threshold = threshold;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id); map.put("itemKey", itemKey); map.put("condition", condition);
        map.put("threshold", threshold); map.put("enabled", enabled); map.put("repeat", repeat);
        map.put("cooldownSeconds", cooldownSeconds); map.put("rearmOnExit", rearmOnExit);
        map.put("notification", notification); map.put("lastTriggeredAtMs", lastTriggeredAtMs);
        map.put("triggered", triggered); map.put("latched", latched);
        return map;
    }

    public static PriceAlertRule fromMap(Map<?, ?> map) {
        PriceAlertRule rule = new PriceAlertRule();
        rule.id = string(map, "id", UUID.randomUUID().toString());
        rule.itemKey = string(map, "itemKey", "");
        rule.condition = string(map, "condition", "BUY_ABOVE");
        rule.threshold = number(map, "threshold", 0);
        rule.enabled = bool(map, "enabled", true); rule.repeat = bool(map, "repeat", false);
        rule.cooldownSeconds = (int) number(map, "cooldownSeconds", 300);
        rule.rearmOnExit = bool(map, "rearmOnExit", true);
        rule.notification = string(map, "notification", "CHAT");
        rule.lastTriggeredAtMs = (long) number(map, "lastTriggeredAtMs", 0);
        rule.triggered = bool(map, "triggered", false); rule.latched = bool(map, "latched", false);
        return rule;
    }

    private static String string(Map<?, ?> m, String k, String d) { Object v=m.get(k); return v instanceof String s ? s : d; }
    private static double number(Map<?, ?> m, String k, double d) { Object v=m.get(k); return v instanceof Number n ? n.doubleValue() : d; }
    private static boolean bool(Map<?, ?> m, String k, boolean d) { Object v=m.get(k); return v instanceof Boolean b ? b : d; }
}
