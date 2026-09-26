package systems.diath.visotaris_opmod.services;

import systems.diath.visotaris_opmod.config.PriceAlertRule;

import java.util.Locale;

/** Allowlist and bounds for the dedicated local-web alert editor. */
public final class PriceAlertInputValidator {
    private PriceAlertInputValidator() { }

    public static PriceAlertRule create(String itemKey, String condition, double threshold, boolean enabled,
                                        boolean repeat, int cooldownSeconds, boolean rearmOnExit, String notification) {
        if (itemKey == null || !itemKey.toLowerCase(Locale.ROOT).matches("[a-z0-9_#:-]{1,96}")) return null;
        if (condition == null || !PriceAlertEngine.CONDITIONS.contains(condition.toUpperCase(Locale.ROOT))) return null;
        if (!Double.isFinite(threshold) || Math.abs(threshold) > 1.0e15) return null;
        if (cooldownSeconds < 10 || cooldownSeconds > 86400) return null;
        String channel = PriceAlertRule.normalizeNotification(notification);
        if (channel == null) return null;
        PriceAlertRule rule = new PriceAlertRule(itemKey.toLowerCase(Locale.ROOT), condition.toUpperCase(Locale.ROOT), threshold);
        rule.enabled = enabled; rule.repeat = repeat; rule.cooldownSeconds = cooldownSeconds;
        rule.rearmOnExit = rearmOnExit; rule.notification = channel;
        return rule;
    }
}
