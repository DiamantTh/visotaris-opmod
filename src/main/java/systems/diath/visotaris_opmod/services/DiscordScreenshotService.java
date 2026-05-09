package systems.diath.visotaris_opmod.services;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.config.VisotarisConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

public final class DiscordScreenshotService {

    public static final int TARGET_COUNT = 5;

    private static final DateTimeFormatter FILE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final MediaType PNG = MediaType.get("image/png");

    private final ConfigManager configManager;
    private final ScreenshotCaptureBackend captureBackend;
    private final ExecutorService executor;
    private volatile OkHttpClient httpClient;

    public DiscordScreenshotService(ConfigManager configManager, ScreenshotCaptureBackend captureBackend) {
        this.configManager = configManager;
        this.captureBackend = captureBackend;
        this.executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }

    public void sendToTarget(int targetIndex) {
        VisotarisConfig cfg = configManager.getConfig();
        if (targetIndex < 0 || targetIndex >= TARGET_COUNT) {
            captureBackend.notifyUser("Discord-Screenshot-Ziel ist ungültig.");
            return;
        }
        if (!cfg.isDiscordScreenshotTargetEnabled(targetIndex)) {
            captureBackend.notifyUser(targetLabel(cfg, targetIndex) + " ist deaktiviert.");
            return;
        }
        String webhookUrl = cfg.getDiscordScreenshotWebhookUrl(targetIndex);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            captureBackend.notifyUser(targetLabel(cfg, targetIndex) + " hat keine Webhook-URL.");
            return;
        }

        String filename = "visotaris-" + FILE_TIME.format(LocalDateTime.now()) + ".png";
        captureBackend.notifyUser("Screenshot wird aufgenommen...");
        captureBackend.capture(
            filename,
            path -> submit(() -> upload(path, targetIndex, webhookUrl.strip())),
            error -> captureBackend.notifyUser("Screenshot fehlgeschlagen: " + error)
        );
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void upload(Path screenshot, int targetIndex, String webhookUrl) {
        VisotarisConfig cfg = configManager.getConfig();
        String label = targetLabel(cfg, targetIndex);
        try {
            if (!Files.isRegularFile(screenshot)) {
                throw new IOException("Datei wurde nicht geschrieben: " + screenshot.getFileName());
            }

            RequestBody fileBody = RequestBody.create(screenshot.toFile(), PNG);
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("files[0]", screenshot.getFileName().toString(), fileBody)
                .build();

            Request request = new Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build();

            try (Response response = httpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    ResponseBody body = response.body();
                    String suffix = body != null ? ": " + body.string() : "";
                    throw new IOException("Discord-Webhook Status " + response.code() + suffix);
                }
            }

            if (!cfg.saveDiscordScreenshotsLocally) {
                Files.deleteIfExists(screenshot);
            }
            captureBackend.notifyUser("Screenshot an " + label + " gesendet.");
        } catch (Exception e) {
            VisotarisLogger.warn("DiscordScreenshot: Upload fehlgeschlagen: {}", e.getMessage());
            captureBackend.notifyUser("Discord-Upload fehlgeschlagen: " + e.getMessage());
        }
    }

    private OkHttpClient httpClient() {
        OkHttpClient existing = httpClient;
        if (existing != null) return existing;
        OkHttpClient created = VisotarisConst.buildOkHttpClient(configManager.getConfig());
        httpClient = created;
        return created;
    }

    private void submit(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // Client-Shutdown gewinnt gegen nachlaufende Keybinds.
        }
    }

    private static String targetLabel(VisotarisConfig cfg, int targetIndex) {
        String name = cfg.getDiscordScreenshotTargetName(targetIndex);
        if (name != null && !name.isBlank()) return name.strip();
        return "Ziel " + (targetIndex + 1);
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "Visotaris-DiscordScreenshot");
            thread.setDaemon(true);
            return thread;
        }
    }
}
