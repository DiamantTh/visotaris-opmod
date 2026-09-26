package systems.diath.visotaris_opmod.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.JobSnapshot;
import systems.diath.visotaris_opmod.model.InventoryValuation;
import systems.diath.visotaris_opmod.services.InventoryValuationService;
import systems.diath.visotaris_opmod.services.JobTrackerService;
import systems.diath.visotaris_opmod.services.PriceAlertNotificationQueue;
import systems.diath.visotaris_opmod.services.PriceAlertService;
import systems.diath.visotaris_opmod.util.ItemNameResolver;

import java.util.Locale;

/**
 * HUD-Overlay: zeigt Job-Tracker-Daten (Job, Level, XP/h, Money/h) sowie die
 * Inventar-voll-Warnung auf dem Screen.
 *
 * Rendern läuft auf dem Client-Render-Thread; kein Netzwerk, kein Blocking.
 * Alle Daten kommen aus {@link JobTrackerService} (atomar gelesen).
 *
 * MC 26.x: {@code HudRenderCallback} (fabric-rendering-v1) wurde durch das
 * {@code HudElement}/{@code HudElementRegistry}-System ersetzt; {@code GuiGraphics}
 * heißt jetzt {@code GuiGraphicsExtractor} und die Render-Methode extrahiert nur noch
 * Zeichenzustand (extractRenderState) statt direkt zu zeichnen.
 */
@Environment(EnvType.CLIENT)
public final class HudOverlay implements HudElement {

    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_JOB   = 0xFFFFD700;

    private final JobTrackerService          jobTracker;
    private final InventoryValuationService  valuation;
    private final ConfigManager              config;
    private final PriceAlertNotificationQueue alertNotifications;
    private PriceAlertService.Event displayedAlert;
    private String alertItem;
    private String alertCondition;
    private String alertValue;

    /** Standard-Position oben links (offset). */
    private int posX = 4;
    private int posY = 4;
    private InventoryValuation cachedInventoryValue = InventoryValuation.empty();
    private long lastInventoryEvaluationMs = 0L;

    public HudOverlay(JobTrackerService jobTracker,
                      InventoryValuationService valuation,
                      ConfigManager config,
                      PriceAlertNotificationQueue alertNotifications) {
        this.jobTracker = jobTracker;
        this.valuation  = valuation;
        this.config     = config;
        this.alertNotifications = alertNotifications;
    }

    /** Wird per HudElementRegistry.addLast(...) registriert. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        var cfg = config.getConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) return;

        if (cfg.priceAlertsEnabled) renderPriceAlert(ctx, mc);
        else alertNotifications.clear();
        if (!cfg.ingameFeaturesEnabled() || !cfg.showHud) return;

        int nextY = renderJobInfo(ctx, mc);
        renderInventoryValue(ctx, mc, nextY);
        if (cfg.enableInventoryWarning) renderInventoryWarning(ctx, mc);
    }

    /** Short-lived price alert card; independent of the persistent job/inventory HUD. */
    private void renderPriceAlert(GuiGraphicsExtractor ctx, Minecraft mc) {
        PriceAlertService.Event alert = alertNotifications.current(System.currentTimeMillis());
        if (alert == null) { displayedAlert = null; return; }
        if (alert != displayedAlert) {
            displayedAlert = alert;
            alertItem = ItemNameResolver.resolve(alert.itemKey());
            alertCondition = PriceAlertService.conditionLabel(alert.condition()) + " " + formatAlertPrice(alert.threshold());
            alertValue = "Aktuell: " + formatAlertPrice(alert.currentValue());
        }

        int width = Math.min(256, mc.getWindow().getGuiScaledWidth() - 16);
        int x = mc.getWindow().getGuiScaledWidth() - width - 8;
        int y = 8;
        ctx.fill(x, y, x + width, y + 54, 0xE9132030);
        ctx.fill(x, y, x + 3, y + 54, 0xFFA3E635);
        ctx.fill(x + 3, y, x + width, y + 1, 0xFF375773);
        ctx.text(mc.font, "PREISALARM", x + 9, y + 5, 0xFFA3E635, true);
        int more = alertNotifications.pendingCount();
        if (more > 0) {
            String count = "+" + more;
            ctx.text(mc.font, count, x + width - mc.font.width(count) - 8, y + 5, 0xFFB7C9D9, true);
        }
        int textWidth = Math.max(0, width - 18);
        ctx.text(mc.font, fitAlertText(mc, alertItem, textWidth), x + 9, y + 17, 0xFFE5EDF2, true);
        ctx.text(mc.font, fitAlertText(mc, alertCondition, textWidth), x + 9, y + 29, 0xFFB7C9D9, true);
        ctx.text(mc.font, fitAlertText(mc, alertValue, textWidth), x + 9, y + 41, 0xFFA3E635, true);
    }

    private static String formatAlertPrice(double price) {
        return String.format(Locale.GERMANY, "%,.2f", price);
    }

    private static String fitAlertText(Minecraft mc, String text, int maxWidth) {
        if (mc.font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && mc.font.width(text + "…") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    // ── Job-Info ────────────────────────────────────────────────────────────

    private int renderJobInfo(GuiGraphicsExtractor ctx, Minecraft mc) {
        JobSnapshot snap = jobTracker.getSnapshot();
        if (snap.getJobName().isBlank()) return posY;

        var font = mc.font;
        int x = posX;
        int y = posY;
        int lineH = font.lineHeight + 2;

        ctx.text(font, "§6" + snap.getJobName().toUpperCase(), x, y, COLOR_JOB, true);
        y += lineH;
        ctx.text(font,
            "Level " + snap.getLevel() + "  §7(" + String.format("%.1f", snap.getPercent()) + "%)",
            x, y, COLOR_VALUE, true);
        y += lineH;
        ctx.text(font, "XP/h: §f" + formatShort(snap.getXpPerHour()),  x, y, COLOR_LABEL, true);
        y += lineH;
        ctx.text(font, "$/h: §f"  + formatShort(snap.getMoneyPerHour()), x, y, COLOR_LABEL, true);
        return y + lineH + 1;
    }

    /** Zeigt den Markt-/Händlerwert des Spielerinventars, höchstens zweimal pro Sekunde neu berechnet. */
    private void renderInventoryValue(GuiGraphicsExtractor ctx, Minecraft mc, int y) {
        long now = System.currentTimeMillis();
        if (now - lastInventoryEvaluationMs >= 500L) {
            cachedInventoryValue = valuation.evaluatePlayerInventory();
            lastInventoryEvaluationMs = now;
        }
        InventoryValuation value = cachedInventoryValue;
        if (value.getBuyTotal() <= 0 && value.getSellTotal() <= 0
                && !value.hasShards() && !value.hasRedcoins()) return;

        StringBuilder line = new StringBuilder("§7Inv:");
        if (value.getSellTotal() > 0) line.append(" §fV ").append(formatShort(value.getSellTotal()));
        if (value.getBuyTotal() > 0) line.append(" §aK ").append(formatShort(value.getBuyTotal()));
        if (value.hasShards()) line.append(" §bS ").append(formatShort(value.getShardTotal()));
        if (value.hasRedcoins()) line.append(" §6R ").append(formatShort(value.getRedcoinTotal()));
        ctx.text(mc.font, line.toString(), posX, y, COLOR_LABEL, true);
    }

    // ── Inventar-voll-Warnung ───────────────────────────────────────────────

    private void renderInventoryWarning(GuiGraphicsExtractor ctx, Minecraft mc) {
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

        ctx.text(font, text, x, y, color, true);
    }

    private static String formatShort(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 1_000)     return String.format("%.1fK", value / 1_000);
        return String.format("%.0f", value);
    }
}
