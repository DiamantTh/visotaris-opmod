package systems.diath.visotaris_opmod.services;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface ScreenshotCaptureBackend {
    void capture(String filename, Consumer<Path> onSaved, Consumer<String> onFailure);

    void notifyUser(String message);
}
