package systems.diath.visotaris_opmod.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.VisotarisModClient;

/**
 * Unter-Screen für Netzwerk-Einstellungen (OPSucht-API, Web-Interface, Proxy).
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

    private EditBox fieldUserAgent;
    private EditBox fieldApiKey;
    private EditBox fieldWebUiPort;
    private EditBox fieldProxyHost;
    private EditBox fieldProxyPort;

    public NetworkSettingsScreen(Screen parent) {
        super(Component.literal("Visotaris \u2013 Netzwerk & Web-Interface"));
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
        fieldUserAgent = new EditBox(this.font, fx, y, fw, FIELD_H,
            Component.literal("User-Agent"));
        fieldUserAgent.setMaxLength(256);
        fieldUserAgent.setSuggestion("leer \u2192 automatisch");
        fieldUserAgent.setValue(cfg.customUserAgent);
        fieldUserAgent.setResponder(s -> {
            cfg.customUserAgent = s;
            // Hinweis-Component nur zeigen, wenn Feld leer ist
            fieldUserAgent.setSuggestion(s.isEmpty() ? "leer \u2192 automatisch" : "");
        });
        this.addRenderableWidget(fieldUserAgent);
        y += FIELD_H + GAP_ROW;

        // ── API-Schlüssel ────────────────────────────────────────────────
        y += 10 + GAP_LABEL;
        fieldApiKey = new EditBox(this.font, fx, y, fw, FIELD_H,
            Component.literal("API-Key"));
        fieldApiKey.setMaxLength(256);
        fieldApiKey.setSuggestion("leer \u2192 kein Bearer-Token");
        fieldApiKey.setValue(cfg.apiKey);
        fieldApiKey.setResponder(s -> {
            cfg.apiKey = s;
            fieldApiKey.setSuggestion(s.isEmpty() ? "leer \u2192 kein Bearer-Token" : "");
        });
        this.addRenderableWidget(fieldApiKey);
        y += FIELD_H + GAP_ROW;

        // ── Web-Interface-Port ───────────────────────────────────────────
        y += 10 + GAP_LABEL;
        fieldWebUiPort = new EditBox(this.font, fx, y, fw, FIELD_H,
            Component.literal("Web-Interface-Port"));
        fieldWebUiPort.setMaxLength(5);
        fieldWebUiPort.setFilter(s -> s.isEmpty() || s.matches("\\d{1,5}"));
        fieldWebUiPort.setValue(String.valueOf(cfg.webUiPort));
        fieldWebUiPort.setResponder(s -> {
            Integer port = parsePort(s);
            if (port != null) {
                cfg.webUiPort = port;
                fieldWebUiPort.setSuggestion("");
            } else {
                fieldWebUiPort.setSuggestion("1-65535");
            }
        });
        this.addRenderableWidget(fieldWebUiPort);
        y += FIELD_H + GAP_ROW;

        // ── Proxy-Host + Port ────────────────────────────────────────────
        y += 10 + GAP_LABEL;
        int portW     = 52;
        int portGap   = 6;
        int hostW     = fw - portW - portGap;

        fieldProxyHost = new EditBox(this.font, fx, y, hostW, FIELD_H,
            Component.literal("Proxy-Host"));
        fieldProxyHost.setMaxLength(256);
        fieldProxyHost.setSuggestion("leer \u2192 kein Proxy");
        fieldProxyHost.setValue(cfg.proxyHost);
        fieldProxyHost.setResponder(s -> {
            cfg.proxyHost = s;
            fieldProxyHost.setSuggestion(s.isEmpty() ? "leer \u2192 kein Proxy" : "");
        });
        this.addRenderableWidget(fieldProxyHost);

        fieldProxyPort = new EditBox(this.font, fx + hostW + portGap, y, portW, FIELD_H,
            Component.literal("Port"));
        fieldProxyPort.setMaxLength(5);
        fieldProxyPort.setSuggestion("Port");
        // Nur Ziffern erlauben
        fieldProxyPort.setFilter(s -> s.isEmpty() || s.matches("\\d{1,5}"));
        fieldProxyPort.setValue(cfg.proxyPort > 0 ? String.valueOf(cfg.proxyPort) : "");
        fieldProxyPort.setResponder(s -> {
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
        this.addRenderableWidget(fieldProxyPort);

        // ── Buttons ──────────────────────────────────────────────────────
        int bw  = 140;
        int bh  = 20;
        int gap = 8;
        int by  = this.height - 28;
        int bx  = this.width / 2 - bw - gap / 2;

        this.addRenderableWidget(Button.builder(
            Component.literal("Speichern & Schlie\u00dfen"),
            b -> {
                Integer webUiPort = parsePort(fieldWebUiPort.getValue());
                if (webUiPort != null) {
                    cfg.webUiPort = webUiPort;
                }
                configManager.save();
                VisotarisModClient.getInstance().applyWebUiConfig();
                this.minecraft.setScreen(parent);
            }
        ).bounds(bx, by, bw, bh).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Abbrechen"),
            b -> {
                configManager.load();
                this.minecraft.setScreen(parent);
            }
        ).bounds(bx + bw + gap, by, bw, bh).build());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Titel
        ctx.fill(0, 0, this.width, 26, 0xAA0D1B3E);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        ctx.fill(this.width / 2 - 110, 20, this.width / 2 + 110, 21, 0x66AAAAAA);
        ctx.drawCenteredString(this.font,
            Component.literal("\u00a7eOPSucht-API, lokales Web-Interface und Proxy"),
            this.width / 2, 28, 0xFFFFFF);

        // Feld-Labels (oberhalb der jeweiligen EditBoxs)
        int lColor = 0xAAAAAA;
        ctx.drawString(this.font,
            Component.literal("Benutzerdefinierter User-Agent"),
            MARGIN, fieldUserAgent.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawString(this.font,
            Component.literal("OPSucht-API-Schl\u00fcssel (Bearer-Auth)"),
            MARGIN, fieldApiKey.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawString(this.font,
            Component.literal("Web-Interface-Port (localhost)"),
            MARGIN, fieldWebUiPort.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawString(this.font,
            Component.literal("Proxy-Host"),
            MARGIN, fieldProxyHost.getY() - 10 - GAP_LABEL, lColor);
        ctx.drawString(this.font,
            Component.literal("Port"),
            fieldProxyPort.getX(), fieldProxyPort.getY() - 10 - GAP_LABEL, lColor);

        // Hinweis
        int noteY = fieldProxyHost.getY() + FIELD_H + 6;
        ctx.drawCenteredString(this.font,
            Component.literal("\u00a77Browser-Port: 127.0.0.1:" + cfg.webUiPort + " (localhost)"),
            this.width / 2, noteY, 0xFFFFFF);
        ctx.drawCenteredString(this.font,
            Component.literal("\u00a77Speichern \u00fcbernimmt den Port direkt."),
            this.width / 2, noteY + this.font.lineHeight + 2, 0xFFFFFF);
    }

    private static Integer parsePort(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65535 ? port : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
