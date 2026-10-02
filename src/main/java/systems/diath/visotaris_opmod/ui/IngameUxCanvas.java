package systems.diath.visotaris_opmod.ui;

/** Small drawing surface shared by the Minecraft 1.21 and 26 screen APIs. */
public interface IngameUxCanvas {
    void fill(int left, int top, int right, int bottom, int color);
    void item(String registryKey, int x, int y);
    void icon(String path, int x, int y, int size);
    void enableScissor(int left, int top, int right, int bottom);
    void disableScissor();
    void text(String value, int x, int y, int color, boolean shadow);
    int textWidth(String value);
    int lineHeight();
}
