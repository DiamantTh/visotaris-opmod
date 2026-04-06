package systems.diath.noopmod.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import systems.diath.noopmod.config.ConfigManager;
import systems.diath.noopmod.model.JobSnapshot;
import systems.diath.noopmod.services.InventoryValuationService;
import systems.diath.noopmod.services.JobTrackerService;

/**
 * HUD-Overlay: zeigt Job-Tracker-Daten (Job, Level, XP/h, Money/h) auf dem Screen.
 *
 * Rendern läuft auf dem Client-Render-Thread; kein Netzwerk, kein Blocking.
 * Alle Daten kommen aus {@link JobTrackerService} (atomar gelesen).
 *
 * Phase 2 TODO:
 *   - Position per Drag-and-Drop konfigurierbar machen (UIEditorScreen)
 *   - Inventarwert-Panel einbauen
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

        JobSnapshot snap = jobTracker.getSnapshot();
        if (snap.getJobName().isBlank()) return;

        var font = mc.textRenderer;
        int x = posX;
        int y = posY;
        int lineH = font.fontHeight + 2;

        // Job-Name
        ctx.drawText(font, "§6" + snap.getJobName().toUpperCase(), x, y, COLOR_JOB, true);
        y += lineH;

        // Level & %
        ctx.drawText(font,
            "Level " + snap.getLevel() + "  §7(" + String.format("%.1f", snap.getPercent()) + "%)",
            x, y, COLOR_VALUE, true);
        y += lineH;

        // XP/h
        ctx.drawText(font,
            "XP/h: §f" + formatShort(snap.getXpPerHour()),
            x, y, COLOR_LABEL, true);
        y += lineH;

        // Money/h
        ctx.drawText(font,
            "$/h: §f" + formatShort(snap.getMoneyPerHour()),
            x, y, COLOR_LABEL, true);
    }

    private static String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
