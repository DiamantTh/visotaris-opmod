package systems.diath.visotaris_opmod.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.diath.visotaris_opmod.VisotarisModClient;
import systems.diath.visotaris_opmod.model.InventoryValuation;
import systems.diath.visotaris_opmod.util.HandledScreenButtons;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin in AbstractContainerScreen:
 *   - Container-Preis-Overlay  (Phase 2): zeigt Verkaufs-/Kaufwert aller Slots
 *   - Schnellzugriff-Buttons   (Phase 2): 3 OPSUCHT-Commands unterhalb des Hintergrunds
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    /** Gibt den AbstractContainerMenu des Screens zurück (Shadow zur Zielklasse). */
    @Shadow public abstract T getMenu();


    // ════════════════════════════════════════════════════════════════════════
    //  INIT – Daten-Fetch beim Öffnen triggern
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Wenn ein Container-Screen geöffnet wird und Overlay oder Tooltips aktiv sind,
     * einen Daten-Fetch anstoßen – aber nur wenn die Cache-Daten tatsächlich veraltet sind.
     * Verhindert API-Hammering beim schnellen Öffnen mehrerer Container nacheinander:
     * {@code refresh()} hat ohnehin einen 60-s-Cooldown; diese Prüfung spart zusätzlich
     * den Executor-Dispatch wenn die Daten offensichtlich noch frisch sind.
     *
     * <p>Schwelle: 5 Minuten – entspricht dem typischen OPSUCHT-Preisupdate-Intervall.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;

        T handler = getMenu();
        boolean isContainer = handler instanceof ChestMenu
                           || handler instanceof ShulkerBoxMenu
                           || handler instanceof InventoryMenu;
        if (!isContainer) return;

        var cfg = mod.getConfigManager().getConfig();
        if (!cfg.ingameFeaturesEnabled()) return;
        if (cfg.showContainerOverlay || cfg.showMarketTooltips) {
            // Cache-Alter prüfen: nur refreshen wenn Daten >5 Minuten alt oder nie geladen
            if (mod.getMarketCache().isStale(300))    mod.getMarketSyncService().refresh();
            if (mod.getShardCache().isStale(300))     mod.getMerchantSyncService().refresh();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;

        // Nur auf Container-Screens (Kisten, Fässer, Shulker) aktivieren.
        // Crafting Table, Ofen, Amboss usw. bleiben unberührt.
        T handler = getMenu();
        boolean isContainer = handler instanceof ChestMenu
                           || handler instanceof ShulkerBoxMenu
                           || handler instanceof InventoryMenu;
        if (!isContainer) return;

        var cfg = mod.getConfigManager().getConfig();
        if (!cfg.ingameFeaturesEnabled()) return;

        if (cfg.showContainerOverlay) renderContainerOverlay(ctx, mod);
        if (cfg.showQuickButtons)     renderQuickButtons(ctx, mouseX, mouseY);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private void renderContainerOverlay(GuiGraphics ctx, VisotarisModClient mod) {
        // Alle Slot-Stacks sammeln
        T handler = getMenu();
        if (handler.slots == null) return;
        List<ItemStack> stacks = new ArrayList<>(handler.slots.size());
        for (Slot slot : handler.slots) stacks.add(slot.getItem());

        InventoryValuation val = mod.getInventoryValuationService().evaluate(stacks);
        if (val.getSellTotal() <= 0 && val.getBuyTotal() <= 0) return;

        // Component zusammenstellen
        String line = buildValueLine(val);

        // Oberhlab des Screen-Hintergrunds in der Titelzeile rendern
        Minecraft mc = Minecraft.getInstance();
        ctx.drawString(mc.font, line,
            leftPos + 4, topPos - mc.font.lineHeight - 2,
            0xFFFFFFFF, true);
    }

    private void renderQuickButtons(GuiGraphics ctx, int mouseX, int mouseY) {
        int total  = HandledScreenButtons.BTN_LABELS.length * HandledScreenButtons.BTN_W
                   + (HandledScreenButtons.BTN_LABELS.length - 1) * HandledScreenButtons.BTN_GAP;
        int startX = leftPos + (imageWidth - total) / 2;
        int rowY   = topPos + imageHeight + 4;

        Minecraft mc = Minecraft.getInstance();
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
            int tw = mc.font.width(HandledScreenButtons.BTN_LABELS[i]);
            int tx = bx + (HandledScreenButtons.BTN_W - tw) / 2;
            int ty = rowY + (HandledScreenButtons.BTN_H - mc.font.lineHeight) / 2;
            ctx.drawString(mc.font, HandledScreenButtons.BTN_LABELS[i], tx, ty, 0xFFFFFFFF, false);
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
