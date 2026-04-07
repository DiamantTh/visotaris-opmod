package systems.diath.visotaris_opmod.util;

/**
 * Gemeinsame Konstanten für die OPSUCHT-Schnellzugriff-Buttons in HandledScreenMixin.
 * Muss außerhalb des hooks-Packages liegen, da Mixin das gesamte hooks-Package beansprucht
 * und keine regulären Klassen darin referenziert werden dürfen.
 */
public final class HandledScreenButtons {

    private HandledScreenButtons() {}

    public static final String[] BTN_LABELS = {"/jobs", "/shard", "/spawn"};
    public static final int      BTN_W      = 54;
    public static final int      BTN_H      = 12;
    public static final int      BTN_GAP    = 3;
}
