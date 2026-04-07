package systems.diath.visotaris.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.JobSnapshot;
import systems.diath.visotaris.services.InventoryValuationService;
import systems.diath.visotaris.services.JobTrackerService;

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
    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!config.getConfig().showHud) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        renderJobInfo(ctx, mc);
        if (config.getConfig().enableInventoryWarning) renderInventoryWarning(ctx, mc);
    }

    // ── Job-Info ────────────────────────────────────────────────────────────

    private void renderJobInfo(DrawContext ctx, MinecraftClient mc) {
        JobSnapshot snap = jobTracker.getSnapshot();
        if (snap.getJobName().isBlank()) return;

        var font = mc.textRenderer;
        int x = posX;
        int y = posY;
        int lineH = font.fontHeight + 2;

        ctx.drawText(font, "§6" + snap.getJobName().toUpperCase(), x, y, COLOR_JOB, true);
        y += lineH;
        ctx.drawText(font,
            "Level " + snap.getLevel() + "  §7(" + String.format("%.1f", snap.getPercent()) + "%)",
            x, y, COLOR_VALUE, true);
        y += lineH;
        ctx.drawText(font, "XP/h: §f" + formatShort(snap.getXpPerHour()),  x, y, COLOR_LABEL, true);
        y += lineH;
        ctx.drawText(font, "$/h: §f"  + formatShort(snap.getMoneyPerHour()), x, y, COLOR_LABEL, true);
    }

    // ── Inventar-voll-Warnung ───────────────────────────────────────────────

    private void renderInventoryWarning(DrawContext ctx, MinecraftClient mc) {
        var inv = mc.player.getInventory();
        // Slots 0–35: Haupt-Inventar + Hotbar
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) return; // mindestens ein freier Slot → kein Alarm
        }

        var font    = mc.textRenderer;
        String text = "§c§lINVENTAR VOLL";

        // Pulsierendes Alpha (0.5–1.0), ~1 Hz
        double pulse  = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 500.0 * Math.PI);
        int    alpha  = (int) (180 + 75 * pulse);   // 180–255
        int    color  = (alpha << 24) | 0x00FF4444; // rötlich

        int tw = font.getWidth(text);
        int x  = (mc.getWindow().getScaledWidth() - tw) / 2;
        // 2 Zeilen über dem Hotbar-Bereich (Hotbar ≈ 22 px vom unteren Rand)
        int y  = mc.getWindow().getScaledHeight() - 22 - font.fontHeight * 2 - 4;

        ctx.drawText(font, text, x, y, color, true);
    }

    private static String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
