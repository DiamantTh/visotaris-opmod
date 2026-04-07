package systems.diath.visotaris.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.diath.visotaris.VisotarisModClient;
import systems.diath.visotaris.model.InventoryValuation;
import systems.diath.visotaris.util.HandledScreenButtons;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin in HandledScreen:
 *   - Container-Preis-Overlay  (Phase 2): zeigt Verkaufs-/Kaufwert aller Slots
 *   - Schnellzugriff-Buttons   (Phase 2): 3 OPSUCHT-Commands unterhalb des Hintergrunds
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    /** Gibt den ScreenHandler des Screens zurück (Shadow zur Zielklasse). */
    @Shadow public abstract T getScreenHandler();


    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;

        // Nur auf Container-Screens (Kisten, Fässer, Shulker) aktivieren.
        // Crafting Table, Ofen, Amboss usw. bleiben unberührt.
        T handler = getScreenHandler();
        boolean isContainer = handler instanceof GenericContainerScreenHandler
                           || handler instanceof ShulkerBoxScreenHandler
                           || handler instanceof PlayerScreenHandler;
        if (!isContainer) return;

        var cfg = mod.getConfigManager().getConfig();

        if (cfg.showContainerOverlay) renderContainerOverlay(ctx, mod);
        if (cfg.showQuickButtons)     renderQuickButtons(ctx, mouseX, mouseY);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private void renderContainerOverlay(DrawContext ctx, VisotarisModClient mod) {
        // Alle Slot-Stacks sammeln
        List<ItemStack> stacks = new ArrayList<>(getScreenHandler().slots.size());
        for (Slot slot : getScreenHandler().slots) stacks.add(slot.getStack());

        InventoryValuation val = mod.getInventoryValuationService().evaluate(stacks);
        if (val.getSellTotal() <= 0 && val.getBuyTotal() <= 0) return;

        // Text zusammenstellen
        String line = buildValueLine(val);

        // Oberhlab des Screen-Hintergrunds in der Titelzeile rendern
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawText(mc.textRenderer, line,
            x + 4, y - mc.textRenderer.fontHeight - 2,
            0xFFFFFFFF, true);
    }

    private void renderQuickButtons(DrawContext ctx, int mouseX, int mouseY) {
        int total  = HandledScreenButtons.BTN_LABELS.length * HandledScreenButtons.BTN_W
                   + (HandledScreenButtons.BTN_LABELS.length - 1) * HandledScreenButtons.BTN_GAP;
        int startX = x + (backgroundWidth - total) / 2;
        int rowY   = y + backgroundHeight + 4;

        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < HandledScreenButtons.BTN_LABELS.length; i++) {
            int bx = startX + i * (HandledScreenButtons.BTN_W + HandledScreenButtons.BTN_GAP);

            boolean hovered = mouseX >= bx && mouseX < bx + HandledScreenButtons.BTN_W
                           && mouseY >= rowY && mouseY < rowY + HandledScreenButtons.BTN_H;

            // Hintergrund
            ctx.fill(bx, rowY, bx + HandledScreenButtons.BTN_W, rowY + HandledScreenButtons.BTN_H,
                hovered ? 0xDD4A4A4A : 0xDD1C1C1C);

            // 1-px-Rand (4 fill-Aufrufe)
            int border = hovered ? 0xFFAAAAAA : 0xFF666666;
            ctx.fill(bx,                           rowY,                              bx + HandledScreenButtons.BTN_W, rowY + 1,                                 border); // oben
            ctx.fill(bx,                           rowY + HandledScreenButtons.BTN_H - 1, bx + HandledScreenButtons.BTN_W, rowY + HandledScreenButtons.BTN_H, border); // unten
            ctx.fill(bx,                           rowY,                              bx + 1,                          rowY + HandledScreenButtons.BTN_H,     border); // links
            ctx.fill(bx + HandledScreenButtons.BTN_W - 1, rowY,                      bx + HandledScreenButtons.BTN_W, rowY + HandledScreenButtons.BTN_H,     border); // rechts

            // Label mittig
            int tw = mc.textRenderer.getWidth(HandledScreenButtons.BTN_LABELS[i]);
            int tx = bx + (HandledScreenButtons.BTN_W - tw) / 2;
            int ty = rowY + (HandledScreenButtons.BTN_H - mc.textRenderer.fontHeight) / 2;
            ctx.drawText(mc.textRenderer, HandledScreenButtons.BTN_LABELS[i], tx, ty, 0xFFFFFFFF, false);
        }
    }

    private static String buildValueLine(InventoryValuation val) {
        StringBuilder sb = new StringBuilder();
        if (val.getSellTotal() > 0) {
            sb.append("§7V: §f").append(fmtVal(val.getSellTotal()));
        }
        if (val.getBuyTotal() > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("§7K: §a").append(fmtVal(val.getBuyTotal()));
        }
        return sb.toString();
    }

    private static String fmtVal(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000);
        return String.format("%.0f", v);
    }
}

