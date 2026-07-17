package systems.diath.visotaris_opmod.hooks;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import systems.diath.visotaris_opmod.VisotarisModClient;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {

    // MC 26.2 hat eine neue grab(Minecraft, boolean)-Überladung mit 2 zusätzlichen Lambdas
    // vor dieser Stelle bekommen -> lambda$grab$1 (26.1.2) ist jetzt lambda$grab$3 (26.2).
    @Inject(
        method = "lambda$grab$3",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",
            shift = At.Shift.AFTER
        ),
        remap = false
    )
    private static void visotaris$sendVanillaScreenshot(NativeImage image, File screenshotFile, Consumer<?> callback, CallbackInfo ci) {
        VisotarisModClient client = VisotarisModClient.getInstance();
        if (client != null && client.getDiscordScreenshotService() != null) {
            client.getDiscordScreenshotService().sendSavedVanillaScreenshot(screenshotFile.toPath());
        }
    }
}
