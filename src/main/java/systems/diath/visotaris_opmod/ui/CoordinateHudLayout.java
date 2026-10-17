package systems.diath.visotaris_opmod.ui;

import net.minecraft.core.BlockPos;
import systems.diath.visotaris_opmod.config.HudPosition;

/** Pure text, visibility and anchor calculations for the compact coordinate HUD. */
public final class CoordinateHudLayout {
    public static final int X_COLOR = 0xFFFF7777;
    public static final int Y_COLOR = 0xFF8ED88F;
    public static final int Z_COLOR = 0xFF72CBF2;

    private static final int ROW_GAP = 2;

    private CoordinateHudLayout() { }

    public static String[] lines(BlockPos position) {
        if (position == null) return new String[0];
        return new String[]{"X  " + position.getX(), "Y  " + position.getY(), "Z  " + position.getZ()};
    }

    public static boolean shouldRender(boolean enabled, boolean inGameFeaturesEnabled,
                                       boolean hudHidden, boolean playerPresent, boolean levelPresent) {
        return enabled && inGameFeaturesEnabled && !hudHidden && playerPresent && levelPresent;
    }

    public static int blockHeight(int fontLineHeight) {
        return fontLineHeight * 3 + ROW_GAP * 2;
    }

    public static int rowY(int originY, int fontLineHeight, int row) {
        return originY + row * (fontLineHeight + ROW_GAP);
    }

    public static Origin origin(int screenWidth, int screenHeight, int textWidth,
                                int fontLineHeight, int margin, HudPosition position) {
        HudPosition anchor = position == null ? HudPosition.LEFT_MIDDLE : position;
        int maxX = Math.max(0, screenWidth - textWidth);
        int x = anchor.rightAligned()
            ? Math.max(0, screenWidth - margin - textWidth)
            : Math.min(Math.max(0, margin), maxX);

        int blockHeight = blockHeight(fontLineHeight);
        int maxY = Math.max(0, screenHeight - blockHeight);
        int y = switch (anchor.verticalAlignment()) {
            case TOP -> Math.min(Math.max(0, margin), maxY);
            case MIDDLE -> Math.max(0, (screenHeight - blockHeight) / 2 - fontLineHeight);
            case BOTTOM -> Math.max(0, screenHeight - margin - blockHeight);
        };
        return new Origin(x, Math.min(y, maxY));
    }

    public record Origin(int x, int y) { }
}
