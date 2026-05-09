package systems.diath.visotaris_opmod.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import systems.diath.visotaris_opmod.VisotarisModClient;

import java.util.ArrayList;
import java.util.List;

public final class DiscordScreenshotSettingsScreen extends Screen {

    private static final int FIELD_H = 18;
    private static final int MARGIN = 20;
    private static final int TARGETS = 5;

    private final Screen parent;
    private final ConfigManager configManager;
    private final VisotarisConfig cfg;
    private final List<Button> targetButtons = new ArrayList<>();
    private EditBox messageField;

    public DiscordScreenshotSettingsScreen(Screen parent) {
        super(Component.literal("Visotaris - Discord-Screenshots"));
        this.parent = parent;
        this.configManager = VisotarisModClient.getInstance().getConfigManager();
        this.cfg = configManager.getConfig();
    }

    @Override
    protected void init() {
        targetButtons.clear();

        int fw = this.width - MARGIN * 2;
        int y = 34;

        this.addRenderableWidget(Button.builder(
            toggleText("Discord-Screenshots", cfg.enableDiscordScreenshots),
            b -> {
                cfg.enableDiscordScreenshots = !cfg.enableDiscordScreenshots;
                b.setMessage(toggleText("Discord-Screenshots", cfg.enableDiscordScreenshots));
            }
        ).bounds(MARGIN, y, fw / 2 - 3, 20).build());

        this.addRenderableWidget(Button.builder(
            toggleText("Lokal speichern", cfg.saveDiscordScreenshotsLocally),
            b -> {
                cfg.saveDiscordScreenshotsLocally = !cfg.saveDiscordScreenshotsLocally;
                b.setMessage(toggleText("Lokal speichern", cfg.saveDiscordScreenshotsLocally));
            }
        ).bounds(MARGIN + fw / 2 + 3, y, fw / 2 - 3, 20).build());
        y += 32;

        messageField = new EditBox(this.font, MARGIN, y, fw, FIELD_H, Component.literal("Nachricht"));
        messageField.setMaxLength(180);
        messageField.setValue(cfg.discordScreenshotMessage);
        messageField.setSuggestion("Discord-Nachricht");
        messageField.setResponder(s -> {
            cfg.discordScreenshotMessage = s;
            messageField.setSuggestion(s.isEmpty() ? "Discord-Nachricht" : "");
        });
        this.addRenderableWidget(messageField);
        y += 30;

        int toggleW = 54;
        int nameW = 78;
        int gap = 6;
        int urlW = fw - toggleW - nameW - gap * 2;
        for (int i = 0; i < TARGETS; i++) {
            int idx = i;
            VisotarisConfig.DiscordScreenshotTarget target = cfg.discordScreenshotTargets[i];

            Button enabledButton = Button.builder(
                toggleText("", target.enabled),
                b -> {
                    target.enabled = !target.enabled;
                    b.setMessage(toggleText("", target.enabled));
                }
            ).bounds(MARGIN, y, toggleW, FIELD_H).build();
            targetButtons.add(enabledButton);
            this.addRenderableWidget(enabledButton);

            EditBox nameField = new EditBox(this.font, MARGIN + toggleW + gap, y, nameW, FIELD_H,
                Component.literal("Name " + (idx + 1)));
            nameField.setMaxLength(32);
            nameField.setValue(target.name);
            nameField.setResponder(s -> target.name = s);
            this.addRenderableWidget(nameField);

            EditBox urlField = new EditBox(this.font, MARGIN + toggleW + gap + nameW + gap, y, urlW, FIELD_H,
                Component.literal("Webhook " + (idx + 1)));
            urlField.setMaxLength(512);
            urlField.setValue(target.webhookUrl);
            urlField.setSuggestion("Discord Webhook URL");
            urlField.setResponder(s -> {
                target.webhookUrl = s;
                urlField.setSuggestion(s.isEmpty() ? "Discord Webhook URL" : "");
            });
            this.addRenderableWidget(urlField);

            y += 24;
        }

        int bw = 140;
        int bh = 20;
        int by = this.height - 28;
        int bx = this.width / 2 - bw - 4;
        this.addRenderableWidget(Button.builder(
            Component.literal("Speichern & Schließen"),
            b -> {
                configManager.save();
                this.minecraft.setScreen(parent);
            }
        ).bounds(bx, by, bw, bh).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Abbrechen"),
            b -> {
                configManager.load();
                this.minecraft.setScreen(parent);
            }
        ).bounds(bx + bw + 8, by, bw, bh).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        ctx.fill(this.width / 2 - 110, 20, this.width / 2 + 110, 21, 0x66AAAAAA);
        ctx.drawString(this.font, Component.literal("Nachricht ({target} wird ersetzt)"),
            MARGIN, messageField.getY() - 10, 0xAAAAAA);
        for (int i = 0; i < targetButtons.size(); i++) {
            Button button = targetButtons.get(i);
            ctx.drawString(this.font, Component.literal("Ziel " + (i + 1)),
                MARGIN, button.getY() - 10, 0xAAAAAA);
        }
    }

    private static Component toggleText(String label, boolean value) {
        String prefix = label == null || label.isBlank() ? "" : label + ": ";
        return Component.literal(prefix + (value ? "§aAN" : "§cAUS"));
    }
}
