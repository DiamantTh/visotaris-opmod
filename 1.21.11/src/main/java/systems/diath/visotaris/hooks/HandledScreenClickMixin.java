package systems.diath.visotaris.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import systems.diath.visotaris.VisotarisModClient;
import systems.diath.visotaris.util.HandledScreenButtons;

/**
 * Version-spezifisches Mixin für MC 1.21.11:
 * mouseClicked hat seit dem GUI-Input-Refactoring die Signatur (Click, boolean).
 * {@code Click} enthält nur den Maus-Button; die Position wird aus {@link MinecraftClient#mouse} bezogen.
 */
@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenClickMixin<T extends ScreenHandler> {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled,
                                CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0) return;
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null || !mod.getConfigManager().getConfig().showQuickButtons) return;

        // In 1.21.11 enthält Click nur button(); GUI-Koordinaten kommen aus der Mouse.
        MinecraftClient mc = MinecraftClient.getInstance();
        var window = mc.getWindow();
        double mouseX = mc.mouse.getX() * window.getScaledWidth()  / (double) window.getWidth();
        double mouseY = mc.mouse.getY() * window.getScaledHeight() / (double) window.getHeight();

        if (handleButtonClick(mouseX, mouseY)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private boolean handleButtonClick(double mouseX, double mouseY) {
        int total  = HandledScreenButtons.BTN_LABELS.length * HandledScreenButtons.BTN_W
                   + (HandledScreenButtons.BTN_LABELS.length - 1) * HandledScreenButtons.BTN_GAP;
        int startX = x + (backgroundWidth - total) / 2;
        int rowY   = y + backgroundHeight + 4;

        for (int i = 0; i < HandledScreenButtons.BTN_LABELS.length; i++) {
            int bx = startX + i * (HandledScreenButtons.BTN_W + HandledScreenButtons.BTN_GAP);
            if (mouseX >= bx && mouseX < bx + HandledScreenButtons.BTN_W
                    && mouseY >= rowY && mouseY < rowY + HandledScreenButtons.BTN_H) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatMessage(HandledScreenButtons.BTN_LABELS[i]);
                }
                return true;
            }
        }
        return false;
    }
}
