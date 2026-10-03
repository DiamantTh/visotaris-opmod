package systems.diath.visotaris_opmod.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import systems.diath.visotaris_opmod.ui.IngameUxCanvas;
import systems.diath.visotaris_opmod.ui.IngameUxScreenBase;

/** Native A+C client menu, rendered with Minecraft 26.x's render-state API. */
public final class VisotarisConfigScreen extends IngameUxScreenBase {
    public VisotarisConfigScreen(Screen parent) { super(parent); }

    @Override
    protected void showScreen(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderUx(new IngameUxCanvas() {
            @Override public void fill(int left, int top, int right, int bottom, int color) {
                graphics.fill(left, top, right, bottom, color);
            }
            @Override public void item(String registryKey, int x, int y) {
                Minecraft client = Minecraft.getInstance();
                // Item defaults are bound to the runtime registry in 26.2. The
                // title screen has no such registry; never construct static stacks there.
                if (client.level == null) {
                    placeholder(x, y);
                    return;
                }
                Identifier id = Identifier.tryParse(registryKey.contains(":") ? registryKey : "minecraft:" + registryKey);
                if (id == null) return;
                var holder = client.level.registryAccess().lookup(Registries.ITEM).flatMap(registry -> registry.get(id));
                if (holder.isPresent() && holder.get().isBound()) {
                    graphics.item(new ItemStack(holder.get()), x, y);
                } else {
                    placeholder(x, y);
                }
            }
            private void placeholder(int x, int y) {
                graphics.fill(x + 2, y + 2, x + 14, y + 14, 0xFF23445B);
                graphics.fill(x + 4, y + 4, x + 12, y + 12, 0xFF88BBCF);
                graphics.fill(x + 6, y + 6, x + 10, y + 10, 0xFF23445B);
            }
            @Override public void icon(String path, int x, int y, int size) {
                graphics.blit(Identifier.fromNamespaceAndPath("visotaris_opmod", "textures/item/" + path + ".png"),
                    x, y, x + size, y + size, 0f, 1f, 0f, 1f);
            }
            @Override public void enableScissor(int left, int top, int right, int bottom) {
                graphics.enableScissor(left, top, right, bottom);
            }
            @Override public void disableScissor() { graphics.disableScissor(); }
            @Override public void text(String value, int x, int y, int color, boolean shadow) {
                graphics.text(font, value, x, y, color, shadow);
            }
            @Override public int textWidth(String value) { return font.width(value); }
            @Override public int lineHeight() { return font.lineHeight; }
        }, mouseX, mouseY, delta);
    }
}
