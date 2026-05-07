package systems.diath.visotaris_opmod.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.JobSnapshot;
import systems.diath.visotaris_opmod.services.InventoryValuationService;
import systems.diath.visotaris_opmod.services.JobTrackerService;

/**
 * HUD-Overlay: zeigt Job-Tracker-Daten (Job, Level, XP/h, Money/h) sowie die
 * Inventar-voll-Warnung auf dem Screen.
 *
 * Rendern läuft auf dem Client-Render-Thread; kein Netzwerk, kein Blocking.
 * Alle Daten kommen aus {@link JobTrackerService} (atomar gelesen).
 */
@Environment(EnvType.CLIENT)
public final class HudOverlay {

    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_JOB   = 0xFFFFD700;

    private final JobTrackerService          jobTracker;
    @SuppressWarnings("unused") // TODO: Inventarwert im HUD anzeigen
    private final InventoryValuationService  valuation;
    private final ConfigManager              config;

    /** Standard-Position oben links (offset). */
    private int posX = 4;
    private int posY = 4;

    public HudOverlay(JobTrackerService jobTracker,
                      InventoryValuationService valuation,
                      ConfigManager config) {
        this.jobTracker = jobTracker;
        this.valuation  = valuation;
        this.config     = config;
    }

    /** Wird per HudRenderCallback.EVENT registriert. */
    public void render(GuiGraphics ctx, DeltaTracker tickCounter) {
        var cfg = config.getConfig();
        if (!cfg.ingameFeaturesEnabled()) return;
        if (!cfg.showHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        renderJobInfo(ctx, mc);
        if (cfg.enableInventoryWarning) renderInventoryWarning(ctx, mc);
    }

    // ── Job-Info ────────────────────────────────────────────────────────────

    private void renderJobInfo(GuiGraphics ctx, Minecraft mc) {
        JobSnapshot snap = jobTracker.getSnapshot();
        if (snap.getJobName().isBlank()) return;

        var font = mc.font;
        int x = posX;
        int y = posY;
        int lineH = font.lineHeight + 2;

        ctx.drawString(font, "§6" + snap.getJobName().toUpperCase(), x, y, COLOR_JOB, true);
        y += lineH;
        ctx.drawString(font,
            "Level " + snap.getLevel() + "  §7(" + String.format("%.1f", snap.getPercent()) + "%)",
            x, y, COLOR_VALUE, true);
        y += lineH;
        ctx.drawString(font, "XP/h: §f" + formatShort(snap.getXpPerHour()),  x, y, COLOR_LABEL, true);
        y += lineH;
        ctx.drawString(font, "$/h: §f"  + formatShort(snap.getMoneyPerHour()), x, y, COLOR_LABEL, true);
    }

    // ── Inventar-voll-Warnung ───────────────────────────────────────────────

    private void renderInventoryWarning(GuiGraphics ctx, Minecraft mc) {
        if (mc.player == null) return;
        var inv = mc.player.getInventory();
        // Slots 0–35: Haupt-Inventar + Hotbar
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) return; // mindestens ein freier Slot → kein Alarm
        }

        var font    = mc.font;
        String text = "§c§lINVENTAR VOLL";

        // Pulsierendes Alpha (0.5–1.0), ~1 Hz
        double pulse  = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 500.0 * Math.PI);
        int    alpha  = (int) (180 + 75 * pulse);   // 180–255
        int    color  = (alpha << 24) | 0x00FF4444; // rötlich

        int tw = font.width(text);
        int x  = (mc.getWindow().getGuiScaledWidth() - tw) / 2;
        // 2 Zeilen über dem Hotbar-Bereich (Hotbar ≈ 22 px vom unteren Rand)
        int y  = mc.getWindow().getGuiScaledHeight() - 22 - font.lineHeight * 2 - 4;

        ctx.drawString(font, text, x, y, color, true);
    }

    private static String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
