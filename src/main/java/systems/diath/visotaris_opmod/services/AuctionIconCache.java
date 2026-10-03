package systems.diath.visotaris_opmod.services;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.config.ConfigManager;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Async, persistent PNG cache keyed by the exact HTTPS icon URL supplied by the auction API. */
public final class AuctionIconCache implements AutoCloseable {
    public static final int MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DIMENSION = 512;
    private static final long NEGATIVE_CACHE_MS = TimeUnit.MINUTES.toMillis(15);

    @FunctionalInterface public interface Downloader { byte[] download(String url) throws Exception; }

    private final Path directory;
    private final Downloader downloader;
    private final ExecutorService workers = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "visotaris-auction-icon");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, byte[]> memory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> failedUntil = new ConcurrentHashMap<>();

    public AuctionIconCache(ConfigManager config) {
        this(VisotarisConst.getCacheDir("auction-icons").toPath(), iconDownloader(config));
    }

    AuctionIconCache(Path directory, Downloader downloader) {
        this.directory = directory;
        this.downloader = downloader;
    }

    public byte[] getCached(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url == null) return null;
        String key = key(url);
        byte[] cached = memory.get(key);
        if (cached != null) return cached;
        Path path = path(key);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_DOWNLOAD_BYTES) return null;
            byte[] bytes = normalizePng(Files.readAllBytes(path));
            memory.putIfAbsent(key, bytes);
            return memory.get(key);
        } catch (Exception invalidFile) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            return null;
        }
    }

    /** Returns immediately; concurrent requests for an identical URL share one future/download. */
    public CompletableFuture<byte[]> request(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url == null) return CompletableFuture.completedFuture(null);
        byte[] cached = getCached(url);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        String key = key(url);
        if (failedUntil.getOrDefault(key, 0L) > System.currentTimeMillis()) return CompletableFuture.completedFuture(null);

        CompletableFuture<byte[]> candidate = new CompletableFuture<>();
        CompletableFuture<byte[]> existing = inFlight.putIfAbsent(key, candidate);
        if (existing != null) return existing;
        try {
            workers.execute(() -> {
                byte[] png = null;
                try {
                    png = normalizePng(downloader.download(url));
                    Files.createDirectories(directory);
                    Files.write(path(key), png);
                    memory.put(key, png);
                    failedUntil.remove(key);
                } catch (Exception failure) {
                    failedUntil.put(key, System.currentTimeMillis() + NEGATIVE_CACHE_MS);
                } finally {
                    candidate.complete(png);
                    inFlight.remove(key, candidate);
                }
            });
        } catch (RuntimeException stopped) {
            inFlight.remove(key, candidate);
            candidate.complete(null);
        }
        return candidate;
    }

    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        try {
            URI uri = URI.create(rawUrl.trim());
            if (!uri.isAbsolute()) uri = URI.create("https://api.opsucht.net").resolve(uri);
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            boolean approvedHost = normalizedHost.equals("opsucht.net") || normalizedHost.endsWith(".opsucht.net")
                || normalizedHost.equals("img.mc-api.io");
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                || !approvedHost
                || (uri.getPort() != -1 && uri.getPort() != 443)) return null;
            return uri.normalize().toString();
        } catch (IllegalArgumentException invalid) { return null; }
    }

    public static String key(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.toString();
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    public static String cacheId(String rawUrl) {
        String normalized = normalizeUrl(rawUrl);
        return normalized == null ? null : key(normalized);
    }

    private Path path(String key) { return directory.resolve(key + ".png"); }

    private static Downloader iconDownloader(ConfigManager config) {
        OkHttpClient http = VisotarisConst.buildOkHttpClient(config.getConfig()).newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).build();
        return url -> {
            String currentUrl = normalizeUrl(url);
            for (int redirects = 0; redirects <= 4; redirects++) {
                Request request = new Request.Builder().url(currentUrl)
                    .header("Accept", "image/png,image/webp,image/jpeg,image/*;q=0.8")
                    .cacheControl(okhttp3.CacheControl.FORCE_NETWORK).build();
                try (Response response = http.newCall(request).execute()) {
                    if (response.code() >= 300 && response.code() < 400) {
                        String location = response.header("Location");
                        String next = location == null ? null : normalizeUrl(URI.create(currentUrl).resolve(location).toString());
                        if (next == null || redirects == 4) throw new IOException("Unsafe or excessive icon redirect");
                        currentUrl = next;
                        continue;
                    }
                    if (!response.isSuccessful() || response.body() == null) throw new IOException("Icon status " + response.code());
                    String contentType = response.header("Content-Type", "").toLowerCase(Locale.ROOT);
                    if (!contentType.startsWith("image/")) throw new IOException("Icon response was not an image");
                    if (response.body().contentLength() > MAX_DOWNLOAD_BYTES) throw new IOException("Icon too large");
                    byte[] bytes = response.body().byteStream().readNBytes(MAX_DOWNLOAD_BYTES + 1);
                    if (bytes.length > MAX_DOWNLOAD_BYTES) throw new IOException("Icon too large");
                    return bytes;
                }
            }
            throw new IOException("Icon redirect limit exceeded");
        };
    }

    private static byte[] normalizePng(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_DOWNLOAD_BYTES) throw new IOException("Invalid icon size");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IOException("Invalid image");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > (long) MAX_DIMENSION * MAX_DIMENSION)
                    throw new IOException("Icon dimensions exceed limit");
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("Icon decode failed");
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder unavailable");
                byte[] png = output.toByteArray();
                if (png.length > MAX_DOWNLOAD_BYTES) throw new IOException("Normalized icon too large");
                return png;
            } finally { reader.dispose(); }
        }
    }

    @Override public void close() { workers.shutdownNow(); }
}
