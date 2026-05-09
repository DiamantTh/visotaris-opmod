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
    private static final String INTERNAL_CAPTURE_PREFIX = "visotaris-";

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
        logVerbose(cfg, "Keybind", targetIndex, webhookUrl);

        String filename = "visotaris-" + FILE_TIME.format(LocalDateTime.now()) + ".png";
        captureBackend.notifyUser("Screenshot wird aufgenommen...");
        captureBackend.capture(
            filename,
            path -> submit(() -> {
                if (upload(path, targetIndex, webhookUrl.strip())) {
                    cleanupIfConfigured(path);
                }
            }),
            error -> captureBackend.notifyUser("Screenshot fehlgeschlagen: " + error)
        );
    }

    public void sendSavedVanillaScreenshot(Path screenshot) {
        if (screenshot == null || isInternalCapture(screenshot)) return;

        VisotarisConfig cfg = configManager.getConfig();
        boolean hasEnabledTarget = false;
        for (int i = 0; i < TARGET_COUNT; i++) {
            if (cfg.isDiscordScreenshotTargetEnabled(i)) {
                hasEnabledTarget = true;
                break;
            }
        }
        if (!hasEnabledTarget) return;

        submit(() -> uploadToEnabledTargets(screenshot));
    }

    public void shutdown() {
        executor.shutdown();
    }

    private void uploadToEnabledTargets(Path screenshot) {
        VisotarisConfig cfg = configManager.getConfig();
        boolean uploaded = false;
        for (int i = 0; i < TARGET_COUNT; i++) {
            if (!cfg.isDiscordScreenshotTargetEnabled(i)) continue;

            String webhookUrl = cfg.getDiscordScreenshotWebhookUrl(i);
            if (webhookUrl == null || webhookUrl.isBlank()) {
                captureBackend.notifyUser(targetLabel(cfg, i) + " hat keine Webhook-URL.");
                continue;
            }
            logVerbose(cfg, "Vanilla-Screenshot", i, webhookUrl);
            uploaded |= upload(screenshot, i, webhookUrl.strip());
        }

        if (uploaded) {
            cleanupIfConfigured(screenshot);
        }
    }

    private boolean upload(Path screenshot, int targetIndex, String webhookUrl) {
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
                .url(withWaitResponse(webhookUrl))
                .post(requestBody)
                .build();

            try (Response response = httpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    ResponseBody body = response.body();
                    String suffix = body != null ? ": " + body.string() : "";
                    throw new IOException("Discord-Webhook Status " + response.code() + suffix);
                }
            }

            captureBackend.notifyUser("Screenshot an " + label + " gesendet.");
            return true;
        } catch (Exception e) {
            VisotarisLogger.warn("DiscordScreenshot: Upload fehlgeschlagen: {}", e.getMessage());
            captureBackend.notifyUser("Discord-Upload fehlgeschlagen: " + e.getMessage());
            return false;
        }
    }

    private void cleanupIfConfigured(Path screenshot) {
        if (configManager.getConfig().saveDiscordScreenshotsLocally) return;
        try {
            Files.deleteIfExists(screenshot);
        } catch (IOException e) {
            VisotarisLogger.warn("DiscordScreenshot: Konnte lokale Datei nicht löschen: {}", e.getMessage());
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

    private static String withWaitResponse(String webhookUrl) {
        if (webhookUrl.contains("wait=")) return webhookUrl;
        return webhookUrl + (webhookUrl.contains("?") ? "&" : "?") + "wait=true";
    }

    private static boolean isInternalCapture(Path screenshot) {
        Path fileName = screenshot.getFileName();
        return fileName != null && fileName.toString().startsWith(INTERNAL_CAPTURE_PREFIX);
    }

    private static void logVerbose(VisotarisConfig cfg, String hook, int targetIndex, String webhookUrl) {
        if (!cfg.verboseDiscordScreenshotLogging) return;
        VisotarisLogger.info(
            "DiscordScreenshot: Hook={} Ziel={} Webhook={}",
            hook,
            targetLabel(cfg, targetIndex),
            redactWebhookUrl(webhookUrl)
        );
    }

    private static String redactWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) return "<leer>";
        String stripped = webhookUrl.strip();
        int keep = Math.min(10, stripped.length());
        return stripped.substring(0, keep) + "...[zensiert]";
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
