package systems.diath.visotaris_opmod.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionRowLayoutTest {
    @Test
    void keepsBalancedColumnsInsideWideRowWithRightMargin() {
        int rowWidth = 520;
        AuctionRowLayout layout = AuctionRowLayout.calculate(rowWidth, 164);

        assertTrue(!layout.stacked());
        assertTrue(layout.leftWidth() >= 92);
        assertTrue(layout.rightWidth() >= 164);
        assertTrue(layout.rightInset() >= 10);
        assertTrue(28 + 10 + layout.leftWidth() + layout.rightWidth() + layout.rightInset() <= rowWidth);
    }

    @Test
    void stacksColumnsBeforeTheLeftTextBecomesUnusable() {
        AuctionRowLayout layout = AuctionRowLayout.calculate(250, 155);

        assertTrue(layout.stacked());
        assertEquals(layout.leftWidth(), layout.rightWidth());
        assertTrue(layout.leftWidth() > 0);
    }
}
