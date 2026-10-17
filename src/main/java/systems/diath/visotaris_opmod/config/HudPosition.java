package systems.diath.visotaris_opmod.config;

import java.util.Locale;

/** Fixed screen anchors for the compact coordinate HUD. */
public enum HudPosition {
    LEFT_TOP("Links oben", false, VerticalAlignment.TOP),
    LEFT_MIDDLE("Links mittig", false, VerticalAlignment.MIDDLE),
    LEFT_BOTTOM("Links unten", false, VerticalAlignment.BOTTOM),
    RIGHT_TOP("Rechts oben", true, VerticalAlignment.TOP),
    RIGHT_MIDDLE("Rechts mittig", true, VerticalAlignment.MIDDLE),
    RIGHT_BOTTOM("Rechts unten", true, VerticalAlignment.BOTTOM);

    public enum VerticalAlignment { TOP, MIDDLE, BOTTOM }

    private final String label;
    private final boolean rightAligned;
    private final VerticalAlignment verticalAlignment;

    HudPosition(String label, boolean rightAligned, VerticalAlignment verticalAlignment) {
        this.label = label;
        this.rightAligned = rightAligned;
        this.verticalAlignment = verticalAlignment;
    }

    public String label() { return label; }
    public boolean rightAligned() { return rightAligned; }
    public VerticalAlignment verticalAlignment() { return verticalAlignment; }

    public HudPosition next() {
        HudPosition[] positions = values();
        return positions[(ordinal() + 1) % positions.length];
    }

    public static HudPosition fromConfig(String value) {
        if (value == null || value.isBlank()) return LEFT_MIDDLE;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return LEFT_MIDDLE; }
    }
}
