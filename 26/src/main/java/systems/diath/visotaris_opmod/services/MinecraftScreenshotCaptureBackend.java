package systems.diath.visotaris_opmod.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class MinecraftScreenshotCaptureBackend implements ScreenshotCaptureBackend {

    @Override
    public void capture(String filename, Consumer<Path> onSaved, Consumer<String> onFailure) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), 1, message ->
                onSaved.accept(mc.gameDirectory.toPath().resolve("screenshots").resolve(filename))
            );
        } catch (Exception e) {
            onFailure.accept(e.getMessage());
        }
    }

    @Override
    public void notifyUser(String message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§e[Visotaris] §7" + message));
            }
        });
    }
}
