package systems.diath.visotaris_opmod.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.VisotarisModClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Einstellungs-Screen der Visotaris Mod.
 *
 * Zweispaltig-Layout für boolsche Optionen; Cycling-Buttons für Intervall-Werte.
 * Der Content-Bereich ist scrollbar (Mausrad). Clip via Scissor.
 * Kompatibel mit MC 1.21.11 (Click-basierte Events):
 * Alle Buttons werden per addRenderableWidget registriert – das MC-eigene Event-Routing
 * nutzt die versionsrichtige Signatur. visible=false blockiert Off-Screen-Buttons.
 *
 * Speichern → configManager.save(); Abbrechen → configManager.load() (Disk-Stand).
 */
public final class VisotarisConfigScreen extends Screen {

    // ── Layout-Konstanten ───────────────────────────────────────────────────
    private static final int BTN_W    = 140;
    private static final int BTN_H    = 18;
    private static final int BTN_GAP  = 4;
    private static final int COL_GAP  = 8;
    private static final int HEADER_H = 24;  // Titel-Bereich (fixiert)
    private static final int FOOTER_H = 36;  // Speichern/Abbrechen (fixiert)
    private static final int SB_W     = 6;   // Scrollbar-Breite
    private static final int SB_PAD   = 2;   // Scrollbar-Rand
    private static final int CAT_H    = 12;  // Höhe einer Kategorie-Überschrift
    private static final int CAT_GAP  = 5;   // Abstand Überschrift → erste Zeile
    private static final int SEC_GAP  = 8;   // Abstand zwischen Kategorien

    // ── Farbpalette (OPSucht: Dunkelblau, Weiß, Neongrün) ─────────────────────
    /** Dunkelblau-Panel über Content-Bereich */
    private static final int COL_BG_PANEL  = 0xCC0D1B3E;
    /** Footer-/Trennlinie */
    private static final int COL_SEPARATOR = 0x881E3A6E;
    /** Scrollbar-Track */
    private static final int COL_SB_TRACK  = 0x661E3A6E;
    /** Scrollbar-Thumb (Hellblau) */
    private static final int COL_SB_THUMB  = 0xBB1E90FF;
    /** Neongrün-Komponenten für Gradient (#39FF14) */
    private static final int C_GREEN_R = 57, C_GREEN_B = 20;

    // Refresh-Presets (Sekunden) für Cycling-Buttons
    private static final int[] REFRESH_PRESETS = {300, 600, 1500}; // 5 / 10 / 25 min

    // ── Zustand ─────────────────────────────────────────────────────────────
    private final Screen          parent;
    private final ConfigManager   configManager;
    private final VisotarisConfig cfg;

    // Content-Buttons (addRenderableWidget für natives Event-Routing; sichtbar nur wenn im Viewport)
    private final List<Button> contentButtons = new ArrayList<>();
    private final List<Integer>      baseYList      = new ArrayList<>();

    // Kategorie-Label-Positionen (werden in render() innerhalb Scissor gezeichnet)
    private final List<Integer> labelYList = new ArrayList<>();
    private final List<String>  labelTexts = new ArrayList<>();

    // Scroll-Zustand
    private int scrollOffset = 0;
    private int maxScroll    = 0;

    // API-Refresh-Status (null = kein Feedback; auto-hide nach 4 s)
    private String refreshStatus   = null;
    private long   refreshStatusMs = 0L;

    public VisotarisConfigScreen(Screen parent) {
        super(Component.literal("Visotaris \u2013 Einstellungen"));
        this.parent        = parent;
        this.configManager = VisotarisModClient.getInstance().getConfigManager();
        this.cfg           = configManager.getConfig();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INIT – Widgets aufbauen
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void init() {
        contentButtons.clear();
        baseYList.clear();
        labelYList.clear();
        labelTexts.clear();
        scrollOffset = 0;

        int cx = this.width / 2;
        int lx = cx - BTN_W - COL_GAP / 2;
        int rx = cx + COL_GAP / 2;
        int fw = BTN_W * 2 + COL_GAP;  // Breite für volle-Breite-Buttons
        int y  = 4;

        // ── Modus ───────────────────────────────────────────────────────────
        addLabel("Modus", y);                                y += CAT_H + CAT_GAP;
        addObserverToggle(cx - fw / 2, y, fw);
        y += BTN_H + BTN_GAP + SEC_GAP;

        // ── Anzeige ─────────────────────────────────────────────────────────
        addLabel("Anzeige", y);                              y += CAT_H + CAT_GAP;
        addToggle(lx, y, "Markt-Tooltips",        cfg.showMarketTooltips,   v -> { cfg.showMarketTooltips = v;   if (v) triggerDataRefresh(); });
        addToggle(rx, y, "HUD anzeigen",           cfg.showHud,              v -> { cfg.showHud = v;              if (v) triggerDataRefresh(); });
        y += BTN_H + BTN_GAP;
        addToggle(lx, y, "Container-Preisanzeige", cfg.showContainerOverlay, v -> { cfg.showContainerOverlay = v; if (v) triggerDataRefresh(); });
        addToggle(rx, y, "Schnellzugriff-Buttons", cfg.showQuickButtons,     v -> cfg.showQuickButtons = v);
        y += BTN_H + BTN_GAP;
        addToggle(lx, y, "Shulker-Rekursion",      cfg.shulkerRecursion,     v -> cfg.shulkerRecursion = v);
        y += BTN_H + BTN_GAP + SEC_GAP;

        // ── Schutz ──────────────────────────────────────────────────────────
        addLabel("Schutz", y);                               y += CAT_H + CAT_GAP;
        addToggle(lx, y, "Rename-Schutz",    cfg.enableRenameProtection, v -> cfg.enableRenameProtection = v);
        addToggle(rx, y, "Sign-Schutz",      cfg.enableSignProtection,   v -> cfg.enableSignProtection = v);
        y += BTN_H + BTN_GAP;
        addToggle(lx, y, "Offhand-Blocker",  cfg.enableOffhandBlocker,   v -> cfg.enableOffhandBlocker = v);
        addToggle(rx, y, "Inventar-Warnung", cfg.enableInventoryWarning, v -> cfg.enableInventoryWarning = v);
        y += BTN_H + BTN_GAP + SEC_GAP;

        // ── Features ────────────────────────────────────────────────────────
        addLabel("Features", y);                             y += CAT_H + CAT_GAP;
        addToggle(lx, y, "Job-Tracker",           cfg.enableJobTracker,         v -> cfg.enableJobTracker = v);
        addToggle(rx, y, "Command-Kurzformen",    cfg.enableCommandShortforms,  v -> cfg.enableCommandShortforms = v,
                Component.literal("\u00a76\u26a0 Kurzformen aktivieren?"),
                Component.literal("Betr\u00e4ge werden automatisch umgewandelt (1k\u21921000).\nFalsche Eingaben k\u00f6nnen fehlerhafte Befehle ausl\u00f6sen!"));
        y += BTN_H + BTN_GAP;
        addToggle(lx, y, "Amboss-Normalisierung", cfg.enableAnvilNormalization, v -> cfg.enableAnvilNormalization = v);
        addToggle(rx, y, "Discord Rich Presence", cfg.enableDiscordRpc,         v -> cfg.enableDiscordRpc = v);
        y += BTN_H + BTN_GAP;
        addContent(Button.builder(
                Component.literal("Discord-Screenshots…"),
                b -> this.minecraft.setScreen(new DiscordScreenshotSettingsScreen(this))
        ).bounds(cx - fw / 2, 0, fw, BTN_H).build(), y);
        y += BTN_H + BTN_GAP + SEC_GAP;

        // ── Netzwerk ────────────────────────────────────────────────────────
        addLabel("Netzwerk", y);                             y += CAT_H + CAT_GAP;
        addCycling(lx, y, "Markt-Refresh",    cfg.marketRefreshIntervalSeconds,    v -> cfg.marketRefreshIntervalSeconds = v);
        addCycling(rx, y, "Merchant-Refresh", cfg.merchantRefreshIntervalSeconds,  v -> cfg.merchantRefreshIntervalSeconds = v);
        y += BTN_H + BTN_GAP + 2;
        addContent(Button.builder(
                Component.literal("\u27f3 API jetzt abrufen"),
                b -> onManualRefresh(b)
        ).bounds(cx - fw / 2, 0, fw, BTN_H).build(), y);
        y += BTN_H + BTN_GAP + 2;
        addContent(Button.builder(
                Component.literal("Netzwerk-Einstellungen\u2026"),
                b -> this.minecraft.setScreen(new NetworkSettingsScreen(this))
        ).bounds(cx - fw / 2, 0, fw, BTN_H).build(), y);
        y += BTN_H + BTN_GAP;

        // ── Scroll-Bereich berechnen ─────────────────────────────────────────
        int contentAreaH = this.height - HEADER_H - FOOTER_H;
        maxScroll = Math.max(0, (y + 4) - contentAreaH);
        updatePositions();

        // ── Fixierte Bottom-Buttons ──────────────────────────────────────────
        int by = this.height - FOOTER_H + 8;
        this.addRenderableWidget(Button.builder(
                Component.literal("Speichern & Schlie\u00dfen"),
                b -> { configManager.save(); this.minecraft.setScreen(parent); }
        ).bounds(cx - BTN_W - COL_GAP / 2, by, BTN_W, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Abbrechen"),
                b -> { configManager.load(); this.minecraft.setScreen(parent); }
        ).bounds(cx + COL_GAP / 2, by, BTN_W, 20).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

@Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // 1. Content-Buttons verstecken damit super.render() sie nicht ungeschnitten zeichnet
        for (Button btn : contentButtons) btn.visible = false;

        // 2. Minecraft-Standardhintergrund + fixierte Bottom-Buttons
        super.render(ctx, mouseX, mouseY, delta);

        // 3. Dunkelblau-Panel über den gesamten Content-Bereich
        int cTop    = HEADER_H;
        int cBottom = this.height - FOOTER_H;
        ctx.fill(0, cTop, this.width, cBottom, COL_BG_PANEL);

        // 4. Trennlinie Footer
        ctx.fill(0, cBottom, this.width, cBottom + 1, COL_SEPARATOR);

        // 5. Sichtbarkeit nach Scroll berechnen
        updatePositions();

        // 6. Content-Buttons mit Scissor-Clipping rendern
        ctx.enableScissor(0, cTop, this.width, cBottom);
        // Kategorie-Überschriften
        for (int i = 0; i < labelYList.size(); i++) {
            int absY = cTop + labelYList.get(i) - scrollOffset;
            if (absY + CAT_H > cTop && absY < cBottom)
                renderCategoryLabel(ctx, labelTexts.get(i), absY);
        }
        for (Button btn : contentButtons) {
            if (btn.visible) btn.render(ctx, mouseX, mouseY, delta);
        }

        // 7. API-Status-Feedback (auto-hide nach 4 s)
        if (refreshStatus != null) {
            if (System.currentTimeMillis() - refreshStatusMs > 4_000L) {
                refreshStatus = null;
            } else {
                int tw = this.font.width(refreshStatus);
                ctx.drawString(this.font, refreshStatus,
                        (this.width - tw) / 2,
                        cBottom - this.font.lineHeight - 6,
                        0xFF39FF14, true);
            }
        }
        ctx.disableScissor();

        // 8. Gradient-Titel + Gradient-Trennlinie (immer über allem)
        renderGradientTitle(ctx);
        renderGradientSeparator(ctx);

        // 9. Scrollbar in OPSucht-Blau
        renderScrollbar(ctx);
    }

    private void renderScrollbar(GuiGraphics ctx) {
        if (maxScroll <= 0) return;
        int cTop = HEADER_H;
        int cH   = this.height - HEADER_H - FOOTER_H;
        int sbX  = this.width - SB_W - SB_PAD;

        float ratio = (float) cH / (cH + maxScroll);
        int   sbH   = Math.max(16, (int) (cH * ratio));
        int   sbY   = cTop + (int) ((cH - sbH) * ((float) scrollOffset / maxScroll));

        ctx.fill(sbX, cTop, sbX + SB_W, cTop + cH, COL_SB_TRACK); // Track (Dunkelblau)
        ctx.fill(sbX, sbY,  sbX + SB_W, sbY + sbH, COL_SB_THUMB); // Thumb (Hellblau)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SCROLL (Mausrad)
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        if (mouseY >= HEADER_H && mouseY < this.height - FOOTER_H) {
            setScroll((int) (scrollOffset - vAmt * 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private void setScroll(int offset) {
        scrollOffset = Math.max(0, Math.min(maxScroll, offset));
        updatePositions();
    }

    /** Setzt absolute Y-Positionen und visible-Flag aller Content-Buttons. */
    private void updatePositions() {
        int cTop    = HEADER_H;
        int cBottom = this.height - FOOTER_H;
        for (int i = 0; i < contentButtons.size(); i++) {
            Button btn  = contentButtons.get(i);
            int          absY = cTop + baseYList.get(i) - scrollOffset;
            btn.setY(absY);
            btn.visible = (absY + BTN_H > cTop) && (absY < cBottom);
        }
    }

    /** Fügt einen Content-Button zu drawables/children (via addRenderableWidget) hinzu. */
    private void addContent(Button btn, int baseY) {
        contentButtons.add(btn);
        baseYList.add(baseY);
        this.addRenderableWidget(btn);
    }

    /** Registriert eine Kategorie-Überschrift (wird in render() gezeichnet). */
    private void addLabel(String text, int baseY) {
        labelYList.add(baseY);
        labelTexts.add(text);
    }

    /** Zeichnet eine Kategorie-Überschrift mit Trenn-Linien links/rechts. */
    private void renderCategoryLabel(GuiGraphics ctx, String text, int absY) {
        int cx   = this.width / 2;
        int tw   = this.font.width(text);
        int ty   = absY + (CAT_H - this.font.lineHeight) / 2;
        ctx.drawCenteredString(this.font, Component.literal(text), cx, ty, 0xFFEEAA33);
        int lineY = ty + this.font.lineHeight / 2;
        ctx.fill(cx - 100,        lineY, cx - tw / 2 - 5, lineY + 1, 0x88EEAA33);
        ctx.fill(cx + tw / 2 + 5, lineY, cx + 100,        lineY + 1, 0x88EEAA33);
    }

    private void addToggle(int x, int baseY, String label, boolean initial, Consumer<Boolean> setter) {
        addToggle(x, baseY, label, initial, setter, null, null);
    }

    /** Volle-Breite-Toggle für den Observer-Modus mit Bestätigungs-Dialog beim Aktivieren. */
    private void addObserverToggle(int x, int baseY, int width) {
        boolean[] state = {cfg.observerModeOnly};
        Button btn = Button.builder(
                makeObserverText(state[0]),
                b -> {
                    boolean enabling = !state[0];
                    if (enabling) {
                        this.minecraft.setScreen(new ConfirmScreen(
                                confirmed -> {
                                    if (confirmed) {
                                        cfg.observerModeOnly = true;
                                    }
                                    this.minecraft.setScreen(VisotarisConfigScreen.this);
                                },
                                Component.literal("\u00a76\u26a0 Observer-Modus aktivieren?"),
                                Component.literal("Deaktiviert ALLE Ingame-Eingriffe (Tooltips, HUD, Container-Overlay,\nSchutzlogik, Job-Tracker, Command-Kurzformen, Discord RPC).\nNur Marktdaten werden weiter abgerufen.")
                        ));
                    } else {
                        state[0] = false;
                        cfg.observerModeOnly = false;
                        b.setMessage(makeObserverText(false));
                    }
                }
        ).bounds(x, 0, width, BTN_H).build();
        addContent(btn, baseY);
    }

    private static Component makeObserverText(boolean v) {
        return Component.literal("Observer-Modus (nur Datenabruf): "
                + (v ? "\u00a7aAN" : "\u00a7cAUS"));
    }

    private void addToggle(int x, int baseY, String label, boolean initial, Consumer<Boolean> setter,
                           Component warnTitle, Component warnMsg) {
        boolean[] state = {initial};
        Button btn = Button.builder(
                makeToggleText(label, initial),
                b -> {
                    boolean enabling = !state[0];
                    if (enabling && warnTitle != null) {
                        this.minecraft.setScreen(new ConfirmScreen(
                                confirmed -> {
                                    if (confirmed) setter.accept(true);
                                    this.minecraft.setScreen(VisotarisConfigScreen.this);
                                },
                                warnTitle, warnMsg
                        ));
                    } else {
                        state[0] = enabling;
                        setter.accept(enabling);
                        b.setMessage(makeToggleText(label, enabling));
                    }
                }
        ).bounds(x, 0, BTN_W, BTN_H).build();
        addContent(btn, baseY);
    }

    private void addCycling(int x, int baseY, String label, int current, Consumer<Integer> setter) {
        int initIdx = 0;
        for (int i = 0; i < REFRESH_PRESETS.length; i++) {
            if (REFRESH_PRESETS[i] == current) { initIdx = i; break; }
        }
        int[] idx = {initIdx};
        Button btn = Button.builder(
                Component.literal(label + ": " + fmtMin(REFRESH_PRESETS[initIdx])),
                b -> {
                    idx[0] = (idx[0] + 1) % REFRESH_PRESETS.length;
                    setter.accept(REFRESH_PRESETS[idx[0]]);
                    b.setMessage(Component.literal(label + ": " + fmtMin(REFRESH_PRESETS[idx[0]])));
                }
        ).bounds(x, 0, BTN_W, BTN_H).build();
        addContent(btn, baseY);
    }

    private static String fmtMin(int seconds) { return (seconds / 60) + "min"; }

    private static Component makeToggleText(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "\u00a7aAN" : "\u00a7cAUS"));
    }

    /**
     * Dreistufiger Gradient: Dunkelblau → Weiß → Neongrün
     */
    private void renderGradientTitle(GuiGraphics ctx) {
        String raw = "Visotaris \u2013 Einstellungen";
        int totalW = this.font.width(raw);
        int x = this.width / 2 - totalW / 2;
        int y = 8;
        int n = raw.length();
        for (int i = 0; i < n; i++) {
            float t = n <= 1 ? 0f : (float) i / (n - 1);
            int r, g, b;
            if (t <= 0.5f) {
                // Dunkelblau (30,80,180) → Weiß
                float s = t / 0.5f;
                r = Math.round(30  + (255 - 30)  * s);
                g = Math.round(80  + (255 - 80)  * s);
                b = Math.round(180 + (255 - 180) * s);
            } else {
                // Weiß → Neongrün #39FF14
                float s = (t - 0.5f) / 0.5f;
                r = Math.round(255 + (C_GREEN_R - 255) * s);
                g = 255;
                b = Math.round(255 + (C_GREEN_B - 255) * s);
            }
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            String ch = String.valueOf(raw.charAt(i));
            ctx.drawString(this.font, ch, x, y, color, true);
            x += this.font.width(ch);
        }
    }

    /**
     * 2-px-Trennlinie: Dunkelblau links → Weiß Mitte → Neongrün rechts.
     */
    private void renderGradientSeparator(GuiGraphics ctx) {
        int x1   = this.width / 2 - 110;
        int x2   = this.width / 2 + 110;
        int span = x2 - x1;
        for (int i = 0; i < span; i++) {
            float t = (float) i / span;
            int r, g, b;
            if (t <= 0.5f) {
                float s = t / 0.5f;
                r = Math.round(30  + (255 - 30)  * s);
                g = Math.round(80  + (255 - 80)  * s);
                b = Math.round(180 + (255 - 180) * s);
            } else {
                float s = (t - 0.5f) / 0.5f;
                r = Math.round(255 + (C_GREEN_R - 255) * s);
                g = 255;
                b = Math.round(255 + (C_GREEN_B - 255) * s);
            }
            int col = 0xCC000000 | (r << 16) | (g << 8) | b;
            ctx.fill(x1 + i, 20, x1 + i + 1, 22, col);
        }
    }

    /** Manueller Refresh-Button: lädt API sofort und zeigt Status-Feedback. */
    private void onManualRefresh(Button btn) {
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;
        btn.active = false;
        btn.setMessage(Component.literal("⏳ Lade..."));
        Thread.ofVirtual().name("visotaris-manual-refresh").start(() -> {
            try {
                mod.getMarketSyncService().refresh();
                mod.getMerchantSyncService().refresh();
                Thread.sleep(800);
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    btn.active = true;
                    btn.setMessage(Component.literal("⟳ API jetzt abrufen"));
                    refreshStatus   = "§a✔ API-Daten aktualisiert";
                    refreshStatusMs = System.currentTimeMillis();
                });
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    btn.active = true;
                    btn.setMessage(Component.literal("⟳ API jetzt abrufen"));
                    refreshStatus   = "§cFehler: " + e.getMessage();
                    refreshStatusMs = System.currentTimeMillis();
                });
            }
        });
    }

    /** Sofortiger Daten-Fetch, z.B. wenn ein Feature aktiviert wird. */
    private static void triggerDataRefresh() {
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;
        mod.getMarketSyncService().refresh();
        mod.getMerchantSyncService().refresh();
    }
}
