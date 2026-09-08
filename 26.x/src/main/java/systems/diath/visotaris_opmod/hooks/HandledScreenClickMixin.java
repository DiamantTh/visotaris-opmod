package systems.diath.visotaris_opmod.hooks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import systems.diath.visotaris_opmod.VisotarisModClient;
import systems.diath.visotaris_opmod.util.HandledScreenButtons;

/** Klick-Handler für die im Container gerenderten Schnellzugriff-Buttons. */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenClickMixin<T extends AbstractContainerMenu> {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled,
                                CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0) return;
        VisotarisModClient mod = VisotarisModClient.getInstance();
        if (mod == null) return;
        var cfg = mod.getConfigManager().getConfig();
        if (!cfg.ingameFeaturesEnabled() || !cfg.showQuickButtons) return;

        if (handleButtonClick(click.x(), click.y())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private boolean handleButtonClick(double mouseX, double mouseY) {
        int total = HandledScreenButtons.BTN_LABELS.length * HandledScreenButtons.BTN_W
            + (HandledScreenButtons.BTN_LABELS.length - 1) * HandledScreenButtons.BTN_GAP;
        int startX = leftPos + (imageWidth - total) / 2;
        int rowY = topPos + imageHeight + 4;

        for (int i = 0; i < HandledScreenButtons.BTN_LABELS.length; i++) {
            int bx = startX + i * (HandledScreenButtons.BTN_W + HandledScreenButtons.BTN_GAP);
            if (mouseX >= bx && mouseX < bx + HandledScreenButtons.BTN_W
                    && mouseY >= rowY && mouseY < rowY + HandledScreenButtons.BTN_H) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    String command = HandledScreenButtons.BTN_LABELS[i];
                    mc.player.connection.sendCommand(command.substring(1));
                }
                return true;
            }
        }
        return false;
    }
}
