package systems.diath.visotaris_opmod.discord.ipc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class DiscordPipeLocator {

    private static final int MAX_PIPE = 10;

    private DiscordPipeLocator() {}

    static Path findUnixPipe() throws DiscordIpcException {
        List<Path> roots = unixCandidateRoots();
        for (int i = 0; i < MAX_PIPE; i++) {
            String name = "discord-ipc-" + i;
            for (Path root : roots) {
                Path path = root.resolve(name);
                if (Files.exists(path)) {
                    return path;
                }
            }
        }
        throw new DiscordIpcException("keine Discord IPC Pipe gefunden");
    }

    static String windowsPipeName(int index) {
        return "\\\\.\\pipe\\discord-ipc-" + index;
    }

    static int maxPipeCount() {
        return MAX_PIPE;
    }

    private static List<Path> unixCandidateRoots() {
        List<Path> roots = new ArrayList<>();
        addEnvPath(roots, "XDG_RUNTIME_DIR");
        addEnvPath(roots, "TMPDIR");
        addPath(roots, Path.of("/tmp"));
        String uid = System.getenv("UID");
        if (uid != null && !uid.isBlank()) {
            addPath(roots, Path.of("/run/user", uid));
        }

        String xdg = System.getenv("XDG_RUNTIME_DIR");
        if (xdg != null && !xdg.isBlank()) {
            Path base = Path.of(xdg);
            addPath(roots, base.resolve("app/com.discordapp.Discord"));
            addPath(roots, base.resolve("app/com.discordapp.DiscordCanary"));
            addPath(roots, base.resolve("app/com.discordapp.DiscordPTB"));
        }
        return roots;
    }

    private static void addEnvPath(List<Path> roots, String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            addPath(roots, Path.of(value));
        }
    }

    private static void addPath(List<Path> roots, Path path) {
        try {
            Path normalized = path.toRealPath();
            if (!roots.contains(normalized)) {
                roots.add(normalized);
            }
        } catch (IOException ignored) {
            // Nicht vorhandene Kandidaten sind normal.
        }
    }
}
