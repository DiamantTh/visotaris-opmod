package systems.diath.visotaris_opmod.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import systems.diath.visotaris_opmod.VisotarisModClient;

import java.util.function.Consumer;

/**
 * Einstellungs-Screen der Visotaris OPMod.
 *
 * Zweispaltig-Layout für boolsche Optionen; Cycling-Buttons für Intervall-Werte.
 * Speichern → configManager.save(); Abbrechen → configManager.load() (Disk-Stand).
 *
 * Eingebunden via ModMenuApiImpl und /noopmod settings.
 */
public final class VisotarisConfigScreen extends Screen {

    // ── Layout-Konstanten ───────────────────────────────────────────────────
    private static final int BTN_W   = 140;
    private static final int BTN_H   = 18;
    private static final int BTN_GAP = 3;
    private static final int COL_GAP = 8;

    // Refresh-Presets (Sekunden) für Cycling-Buttons
    private static final int[] REFRESH_PRESETS = {15, 30, 60, 120, 300};

    // ── Zustand ─────────────────────────────────────────────────────────────
    private final Screen        parent;
    private final ConfigManager configManager;
    private final VisotarisConfig    cfg;

    public VisotarisConfigScreen(Screen parent) {
        super(Text.literal("Visotaris OPMod \u2013 Einstellungen"));
        this.parent        = parent;
        this.configManager = VisotarisModClient.getInstance().getConfigManager();
        this.cfg           = configManager.getConfig();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INIT – Widgets aufbauen
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void init() {
        int cx = this.width / 2;

        // Linke Spalte: Anzeige-Optionen
        int lx = cx - BTN_W - COL_GAP / 2;
        int y  = 36;

        toggle(lx, y, "Markt-Tooltips",         cfg.showMarketTooltips,
               v -> cfg.showMarketTooltips = v);       y += BTN_H + BTN_GAP;
        toggle(lx, y, "HUD anzeigen",            cfg.showHud,
               v -> cfg.showHud = v);                  y += BTN_H + BTN_GAP;
        toggle(lx, y, "Container-Preisanzeige",  cfg.showContainerOverlay,
               v -> cfg.showContainerOverlay = v);     y += BTN_H + BTN_GAP;
        toggle(lx, y, "Schnellzugriff-Buttons",  cfg.showQuickButtons,
               v -> cfg.showQuickButtons = v);         y += BTN_H + BTN_GAP;
        toggle(lx, y, "Shulker-Rekursion",       cfg.shulkerRecursion,
               v -> cfg.shulkerRecursion = v);         y += BTN_H + BTN_GAP;
        toggle(lx, y, "Amboss-Normalisierung",   cfg.enableAnvilNormalization,
               v -> cfg.enableAnvilNormalization = v); y += BTN_H + BTN_GAP;

        // Rechte Spalte: Schutz & Tracker
        int rx = cx + COL_GAP / 2;
        y = 36;

        toggle(rx, y, "Job-Tracker",             cfg.enableJobTracker,
               v -> cfg.enableJobTracker = v);         y += BTN_H + BTN_GAP;
        toggle(rx, y, "Rename-Schutz",           cfg.enableRenameProtection,
               v -> cfg.enableRenameProtection = v);   y += BTN_H + BTN_GAP;
        toggle(rx, y, "Sign-Schutz",             cfg.enableSignProtection,
               v -> cfg.enableSignProtection = v);     y += BTN_H + BTN_GAP;
        toggle(rx, y, "Offhand-Blocker",         cfg.enableOffhandBlocker,
               v -> cfg.enableOffhandBlocker = v);     y += BTN_H + BTN_GAP;
        toggle(rx, y, "Inventar-Warnung",        cfg.enableInventoryWarning,
               v -> cfg.enableInventoryWarning = v);   y += BTN_H + BTN_GAP;
        toggle(rx, y, "Command-Kurzformen",      cfg.enableCommandShortforms,
               v -> cfg.enableCommandShortforms = v);  y += BTN_H + BTN_GAP;
        toggle(rx, y, "Discord Rich Presence",   cfg.enableDiscordRpc,
               v -> cfg.enableDiscordRpc = v);

        // Interval-Cycling (zentriert unterhalb der Spalten)
        // Linke Spalte hat 6 Einträge, rechte 7 → nach der längeren ausrichten
        int iy = 36 + 7 * (BTN_H + BTN_GAP) + 10;
        addCycling(cx - BTN_W - COL_GAP / 2, iy, "Markt-Refresh",
            cfg.marketRefreshIntervalSeconds, v -> cfg.marketRefreshIntervalSeconds = v);
        addCycling(cx + COL_GAP / 2, iy, "Merchant-Refresh",
            cfg.merchantRefreshIntervalSeconds, v -> cfg.merchantRefreshIntervalSeconds = v);

        // Netzwerk-Einstellungen (öffnet Unter-Screen mit TextField-Eingaben)
        int ny     = iy + BTN_H + BTN_GAP + 8;
        int netBtnW = BTN_W * 2 + COL_GAP;
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Netzwerk-Einstellungen\u2026"),
            b -> this.client.setScreen(new NetworkSettingsScreen(this))
        ).dimensions(cx - netBtnW / 2, ny, netBtnW, BTN_H).build());

        // Bottom-Buttons
        int by = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Speichern & Schlie\u00dfen"),
            b -> {
                configManager.save();
                this.client.setScreen(parent);
            }
        ).dimensions(cx - BTN_W - COL_GAP / 2, by, BTN_W, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Abbrechen"),
            b -> {
                configManager.load();           // letzte gespeicherte Werte restaurieren
                this.client.setScreen(parent);
            }
        ).dimensions(cx + COL_GAP / 2, by, BTN_W, 20).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        // Titel
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        // Trennlinie unterhalb Titel
        ctx.fill(this.width / 2 - 100, 20, this.width / 2 + 100, 21, 0x66AAAAAA);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /** Erstellt einen AN/AUS-Toggle-Button und registriert ihn. */
    private void toggle(int x, int y, String label, boolean initial, Consumer<Boolean> setter) {
        boolean[] state = {initial};
        ButtonWidget btn = ButtonWidget.builder(
            makeToggleText(label, initial),
            b -> {
                state[0] = !state[0];
                setter.accept(state[0]);
                b.setMessage(makeToggleText(label, state[0]));
            }
        ).dimensions(x, y, BTN_W, BTN_H).build();
        this.addDrawableChild(btn);
    }

    /** Erstellt einen Cycling-Button für Intervall-Werte (Presets). */
    private void addCycling(int x, int y, String label, int current, Consumer<Integer> setter) {
        int initIdx = 0;
        for (int i = 0; i < REFRESH_PRESETS.length; i++) {
            if (REFRESH_PRESETS[i] == current) { initIdx = i; break; }
        }
        int[] idx = {initIdx};
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(label + ": " + REFRESH_PRESETS[idx[0]] + "s"),
            b -> {
                idx[0] = (idx[0] + 1) % REFRESH_PRESETS.length;
                setter.accept(REFRESH_PRESETS[idx[0]]);
                b.setMessage(Text.literal(label + ": " + REFRESH_PRESETS[idx[0]] + "s"));
            }
        ).dimensions(x, y, BTN_W, BTN_H).build());
    }

    private static Text makeToggleText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "\u00a7aAN" : "\u00a7cAUS"));
    }
}
