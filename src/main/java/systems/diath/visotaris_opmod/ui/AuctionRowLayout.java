package systems.diath.visotaris_opmod.ui;

/** Width-aware columns for an auction row, measured in Minecraft GUI pixels. */
record AuctionRowLayout(int leftWidth, int rightWidth, int rightInset, boolean stacked) {
    private static final int ITEM_TEXT_INSET = 28;
    private static final int COLUMN_GAP = 10;
    private static final int MIN_LEFT_WIDTH = 92;
    private static final int MIN_RIGHT_WIDTH = 88;
    private static final int MAX_RIGHT_WIDTH = 190;
    private static final int ROW_RIGHT_INSET = 12;
    private static final int STACKED_ROW_THRESHOLD = 280;

    static AuctionRowLayout calculate(int rowWidth, int widestPriceText) {
        int width = Math.max(0, rowWidth);
        if (width < STACKED_ROW_THRESHOLD) {
            return new AuctionRowLayout(Math.max(1, width - ITEM_TEXT_INSET - 8),
                Math.max(1, width - ITEM_TEXT_INSET - 8), ROW_RIGHT_INSET, true);
        }

        int maxRightWidth = width - ITEM_TEXT_INSET - COLUMN_GAP - MIN_LEFT_WIDTH - ROW_RIGHT_INSET;
        int rightWidth = Math.min(Math.min(MAX_RIGHT_WIDTH, maxRightWidth),
            Math.max(MIN_RIGHT_WIDTH, widestPriceText + 4));
        int leftWidth = width - ITEM_TEXT_INSET - COLUMN_GAP - rightWidth - ROW_RIGHT_INSET;
        return new AuctionRowLayout(Math.max(1, leftWidth), Math.max(1, rightWidth), ROW_RIGHT_INSET, false);
    }
}
