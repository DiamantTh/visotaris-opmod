package systems.diath.visotaris.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import systems.diath.visotaris.VisotarisModClient;

/**
 * Unter-Screen für Netzwerk-Einstellungen (User-Agent, API-Key, Proxy).
 *
 * <p>Wird aus {@link VisotarisConfigScreen} geöffnet.
 * Änderungen werden sofort in {@link VisotarisConfig} geschrieben;
 * das endgültige Speichern auf Disk übernimmt der übergeordnete ConfigScreen.
 */
public final class NetworkSettingsScreen extends Screen {

    // ── Layout ──────────────────────────────────────────────────────────────
    private static final int FIELD_H   = 18;
    private static final int GAP_LABEL = 2;   // Abstand Label → Feld
    private static final int GAP_ROW   = 12;  // Abstand Feld → nächstes Label
    private static final int MARGIN    = 20;  // Horizontaler Rand

    // ── Zustand ─────────────────────────────────────────────────────────────
    private final Screen        parent;
    private final ConfigManager configManager;
    private final VisotarisConfig    cfg;

    private TextFieldWidget fieldUserAgent;
    private TextFieldWidget fieldApiKey;
    private TextFieldWidget fieldProxyHost;
    private TextFieldWidget fieldProxyPort;

    public NetworkSettingsScreen(Screen parent) {
        super(Text.literal("Visotaris \u2013 Netzwerk-Einstellungen"));
        this.parent        = parent;
        this.configManager = VisotarisModClient.getInstance().getConfigManager();
        this.cfg           = configManager.getConfig();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void init() {
        int fx = MARGIN;
        int fw = this.width - MARGIN * 2;
        int y  = 28;

        // ── Custom User-Agent ────────────────────────────────────────────
        y += GAP_ROW; // Abstand nach dem Titel
        y += 10;      // Label-Höhe (wird beim render() gezeichnet, hier nur vertikaler Platz)
        y += GAP_LABEL;
        fieldUserAgent = new TextFieldWidget(this.textRenderer, fx, y, fw, FIELD_H,
            Text.literal("User-Agent"));
        fieldUserAgent.setMaxLength(256);
        fieldUserAgent.setSuggestion("leer \u2192 automatisch");
        fieldUserAgent.setText(cfg.customUserAgent);
        fieldUserAgent.setChangedListener(s -> {
            cfg.customUserAgent = s;
            // Hinweis-Text nur zeigen, wenn Feld leer ist
            fieldUserAgent.setSuggestion(s.isEmpty() ? "leer \u2192 automatisch" : "");
        });
        this.addDrawableChild(fieldUserAgent);
        y += FIELD_H + GAP_ROW;

        // ── API-Schlüssel ────────────────────────────────────────────────
        y += 10 + GAP_LABEL;
        fieldApiKey = new TextFieldWidget(this.textRenderer, fx, y, fw, FIELD_H,
            Text.literal("API-Key"));
        fieldApiKey.setMaxLength(256);
        fieldApiKey.setSuggestion("leer \u2192 kein Bearer-Token");
        fieldApiKey.setText(cfg.apiKey);
        fieldApiKey.setChangedListener(s -> {
            cfg.apiKey = s;
            fieldApiKey.setSuggestion(s.isEmpty() ? "leer \u2192 kein Bearer-Token" : "");
        });
        this.addDrawableChild(fieldApiKey);
        y += FIELD_H + GAP_ROW;

        // ── Proxy-Host + Port ────────────────────────────────────────────
        y += 10 + GAP_LABEL;
        int portW     = 52;
        int portGap   = 6;
        int hostW     = fw - portW - portGap;

        fieldProxyHost = new TextFieldWidget(this.textRenderer, fx, y, hostW, FIELD_H,
            Text.literal("Proxy-Host"));
        fieldProxyHost.setMaxLength(256);
        fieldProxyHost.setSuggestion("leer \u2192 kein Proxy");
        fieldProxyHost.setText(cfg.proxyHost);
        fieldProxyHost.setChangedListener(s -> {
            cfg.proxyHost = s;
            fieldProxyHost.setSuggestion(s.isEmpty() ? "leer \u2192 kein Proxy" : "");
        });
        this.addDrawableChild(fieldProxyHost);

        fieldProxyPort = new TextFieldWidget(this.textRenderer, fx + hostW + portGap, y, portW, FIELD_H,
            Text.literal("Port"));
        fieldProxyPort.setMaxLength(5);
        fieldProxyPort.setSuggestion("Port");
        // Nur Ziffern erlauben
        fieldProxyPort.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{1,5}"));
        fieldProxyPort.setText(cfg.proxyPort > 0 ? String.valueOf(cfg.proxyPort) : "");
        fieldProxyPort.setChangedListener(s -> {
            if (s.isEmpty()) {
                cfg.proxyPort = 0;
                fieldProxyPort.setSuggestion("Port");
            } else {
                try {
                    cfg.proxyPort = Integer.parseInt(s);
                    fieldProxyPort.setSuggestion("");
                } catch (NumberFormatException ignored) {}
            }
        });
        this.addDrawableChild(fieldProxyPort);

        // ── Buttons ──────────────────────────────────────────────────────
        int bw  = 140;
        int bh  = 20;
        int gap = 8;
        int by  = this.height - 28;
        int bx  = this.width / 2 - bw - gap / 2;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Speichern & Schlie\u00dfen"),
            b -> {
                configManager.save();
                this.client.setScreen(parent);
            }
        ).dimensions(bx, by, bw, bh).build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Abbrechen"),
            b -> {
                configManager.load();
                this.client.setScreen(parent);
            }
        ).dimensions(bx + bw + gap, by, bw, bh).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Titel
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        ctx.fill(this.width / 2 - 110, 20, this.width / 2 + 110, 21, 0x66AAAAAA);

        // Feld-Labels (oberhalb der jeweiligen TextFieldWidgets)
        int lColor = 0xAAAAAA;
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Benutzerdefinierter User-Agent"),
            MARGIN, fieldUserAgent.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("API-Schl\u00fcssel (Bearer-Auth)"),
            MARGIN, fieldApiKey.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Proxy-Host"),
            MARGIN, fieldProxyHost.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Port"),
            fieldProxyPort.getX(), fieldProxyPort.getY() - 10 - GAP_LABEL, lColor);

        // Hinweis
        int noteY = fieldProxyHost.getY() + FIELD_H + 6;
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("\u00a77\u00c4nderungen erfordern einen Neustart des Clients."),
            this.width / 2, noteY, 0xFFFFFF);
    }

}
