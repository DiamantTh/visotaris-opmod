package systems.diath.visotaris_opmod.ui;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.config.HudPosition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateHudLayoutTest {
    @Test void storedPositionNamesAreCaseInsensitiveAndUnknownValuesUseDefault() {
        assertEquals(HudPosition.RIGHT_BOTTOM, HudPosition.fromConfig("right_bottom"));
        assertEquals(HudPosition.LEFT_MIDDLE, HudPosition.fromConfig("future_position"));
        assertEquals(HudPosition.RIGHT_TOP, HudPosition.LEFT_BOTTOM.next());
    }

    @Test void formatsPositiveBlockCoordinatesAsVerticalIntegerRows() {
        assertArrayEquals(new String[]{"X  720", "Y  70", "Z  831"},
            CoordinateHudLayout.lines(BlockPos.containing(720.9, 70.8, 831.1)));
    }

    @Test void usesMinecraftFlooringForNegativeBlockCoordinates() {
        BlockPos position = BlockPos.containing(-172513.168, 28.0, -399187.415);
        assertArrayEquals(new String[]{"X  -172514", "Y  28", "Z  -399188"},
            CoordinateHudLayout.lines(position));
    }

    @Test void missingPositionProducesNoRows() {
        assertArrayEquals(new String[0], CoordinateHudLayout.lines(null));
    }

    @Test void rendererVisibilityRequiresEnabledFeatureWorldPlayerAndVisibleHud() {
        assertTrue(CoordinateHudLayout.shouldRender(true, true, false, true, true));
        assertFalse(CoordinateHudLayout.shouldRender(false, true, false, true, true));
        assertFalse(CoordinateHudLayout.shouldRender(true, false, false, true, true));
        assertFalse(CoordinateHudLayout.shouldRender(true, true, true, true, true));
        assertFalse(CoordinateHudLayout.shouldRender(true, true, false, false, true));
        assertFalse(CoordinateHudLayout.shouldRender(true, true, false, true, false));
    }

    @Test void everyAnchorKeepsTheRowsInsideACompactScaledViewport() {
        int width = 320;
        int height = 180;
        int textWidth = 76;
        int lineHeight = 9;
        int blockHeight = CoordinateHudLayout.blockHeight(lineHeight);

        for (HudPosition position : HudPosition.values()) {
            var origin = CoordinateHudLayout.origin(width, height, textWidth, lineHeight, 8, position);
            assertTrue(origin.x() >= 0 && origin.x() + textWidth <= width, position.name());
            assertTrue(origin.y() >= 0 && origin.y() + blockHeight <= height, position.name());
        }

        assertTrue(CoordinateHudLayout.origin(width, height, textWidth, lineHeight, 8,
            HudPosition.RIGHT_MIDDLE).x() > CoordinateHudLayout.origin(width, height, textWidth, lineHeight, 8,
            HudPosition.LEFT_MIDDLE).x());
    }
}
