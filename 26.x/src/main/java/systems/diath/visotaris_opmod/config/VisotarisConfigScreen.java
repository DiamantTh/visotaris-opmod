package systems.diath.visotaris_opmod.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
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
                Identifier id = Identifier.tryParse(registryKey.contains(":") ? registryKey : "minecraft:" + registryKey);
                if (id == null) return;
                var item = BuiltInRegistries.ITEM.getValue(id);
                if (item != null && !item.equals(net.minecraft.world.item.Items.AIR)) graphics.item(new ItemStack(item), x, y);
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
