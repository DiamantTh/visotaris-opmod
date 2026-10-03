package systems.diath.visotaris_opmod.ui;

/** Drawing surface for the native Minecraft 26.x menu. */
public interface IngameUxCanvas {
    void fill(int left, int top, int right, int bottom, int color);
    void item(String registryKey, int x, int y);
    default boolean image(String key, byte[] png, int x, int y, int size) { return false; }
    void icon(String path, int x, int y, int size);
    void enableScissor(int left, int top, int right, int bottom);
    void disableScissor();
    void text(String value, int x, int y, int color, boolean shadow);
    int textWidth(String value);
    int lineHeight();
}
