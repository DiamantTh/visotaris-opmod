package systems.diath.visotaris_opmod.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Persistent, asynchronous UUID-to-name cache shared by both auction UIs. */
public final class ProfileCache implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long NAME_TTL_MS = TimeUnit.DAYS.toMillis(30);
    private static final long NEGATIVE_TTL_MS = TimeUnit.MINUTES.toMillis(15);

    @FunctionalInterface public interface NameResolver { String resolve(String uuid) throws Exception; }

    private final Path file;
    private final NameResolver minecraftResolver;
    private final NameResolver fallbackResolver;
    private final ExecutorService workers = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "visotaris-player-profile");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong nameVersion = new java.util.concurrent.atomic.AtomicLong();

    public ProfileCache(ConfigManager config) {
        this(VisotarisConst.getCacheDir("profiles").toPath().resolve("players.json"),
            ProfileCache::resolveWithMinecraft, fallbackResolver(config));
    }

    ProfileCache(Path file, NameResolver minecraftResolver, NameResolver fallbackResolver) {
        this.file = file;
        this.minecraftResolver = minecraftResolver;
        this.fallbackResolver = fallbackResolver;
        load();
    }

    /** Returns a cached name or a future that resolves without blocking either UI. */
    public CompletableFuture<String> resolve(String rawUuid) {
        String uuid = canonicalUuid(rawUuid);
        if (uuid == null) return CompletableFuture.completedFuture(shortUuid(rawUuid));
        Entry cached = entries.get(uuid);
        long now = System.currentTimeMillis();
        if (cached != null && cached.name != null && !cached.name.isBlank()) {
            if (now - cached.resolvedAtMs > NAME_TTL_MS) startResolve(uuid);
            return CompletableFuture.completedFuture(cached.name);
        }
        if (cached != null && cached.retryAfterMs > now) return CompletableFuture.completedFuture(shortUuid(uuid));
        return startResolve(uuid);
    }

    /** Read-only lookup for render paths; never initiates I/O. */
    public String getDisplayName(String rawUuid) {
        String uuid = canonicalUuid(rawUuid);
        if (uuid == null) return shortUuid(rawUuid);
        String name = getCachedName(uuid);
        return name == null ? shortUuid(uuid) : name;
    }

    /** Returns only a resolved, locally cached name; never starts a lookup. */
    public String getCachedName(String rawUuid) {
        String uuid = canonicalUuid(rawUuid);
        if (uuid == null) return null;
        Entry entry = entries.get(uuid);
        return entry != null && entry.name != null && !entry.name.isBlank() ? entry.name : null;
    }

    /** Increments when a resolved name is loaded or changes, for local UI-index refreshes. */
    public long getNameVersion() { return nameVersion.get(); }

    public int size() { return entries.size(); }

    private CompletableFuture<String> startResolve(String uuid) {
        CompletableFuture<String> candidate = new CompletableFuture<>();
        CompletableFuture<String> existing = inFlight.putIfAbsent(uuid, candidate);
        if (existing != null) return existing;
        try {
            workers.execute(() -> {
                String name = null;
                try { name = validName(minecraftResolver.resolve(uuid)); }
                catch (Exception ignored) { }
                if (name == null) {
                    try { name = validName(fallbackResolver.resolve(uuid)); }
                    catch (Exception ignored) { }
                }
                if (name == null) entries.put(uuid, new Entry(uuid, null, System.currentTimeMillis(),
                    System.currentTimeMillis() + NEGATIVE_TTL_MS));
                else {
                    Entry previous = entries.put(uuid, new Entry(uuid, name, System.currentTimeMillis(), 0));
                    if (previous == null || !name.equals(previous.name)) nameVersion.incrementAndGet();
                }
                save();
                candidate.complete(name == null ? shortUuid(uuid) : name);
                inFlight.remove(uuid, candidate);
            });
        } catch (RuntimeException stopped) {
            inFlight.remove(uuid, candidate);
            candidate.complete(shortUuid(uuid));
        }
        return candidate;
    }

    private static String resolveWithMinecraft(String rawUuid) {
        UUID uuid = UUID.fromString(rawUuid);
        return Minecraft.getInstance().services().profileResolver().fetchById(uuid)
            .map(com.mojang.authlib.GameProfile::name).orElse(null);
    }

    private static NameResolver fallbackResolver(ConfigManager config) {
        OkHttpClient http = VisotarisConst.buildOkHttpClient(config.getConfig()).newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).build();
        return rawUuid -> {
            String compact = rawUuid.replace("-", "");
            Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" + compact)
                .header("Accept", "application/json").build();
            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                JsonObject profile = JsonParser.parseString(response.body().string()).getAsJsonObject();
                return profile.has("name") ? profile.get("name").getAsString() : null;
            }
        };
    }

    public static String canonicalUuid(String value) {
        if (value == null) return null;
        String compact = value.replace("-", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (!compact.matches("(?i)[0-9a-f]{32}")) return null;
        return compact.substring(0, 8) + "-" + compact.substring(8, 12) + "-" + compact.substring(12, 16)
            + "-" + compact.substring(16, 20) + "-" + compact.substring(20).toLowerCase(java.util.Locale.ROOT);
    }

    public static String shortUuid(String value) {
        if (value == null || value.isBlank()) return "Unbekannt";
        String compact = value.replace("-", "");
        return compact.length() <= 12 ? compact : compact.substring(0, 8) + "…" + compact.substring(compact.length() - 4);
    }

    private static String validName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}") ? value : null;
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            Entry[] stored = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Entry[].class);
            if (stored != null) for (Entry entry : stored) {
                String uuid = entry == null ? null : canonicalUuid(entry.uuid);
                if (uuid != null && ((entry.name != null && validName(entry.name) != null) || entry.retryAfterMs > 0)) {
                    entries.put(uuid, entry);
                    if (entry.name != null && validName(entry.name) != null) nameVersion.incrementAndGet();
                }
            }
        } catch (Exception ignored) { /* A corrupt local profile cache is rebuilt on demand. */ }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "profiles-", ".json.tmp");
            try {
                Files.writeString(temporary, GSON.toJson(entries.values()), StandardCharsets.UTF_8);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException unsupportedAtomicMove) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException ignored) { /* Name resolution remains usable in memory. */ }
    }

    @Override public void close() { workers.shutdownNow(); }

    private static final class Entry {
        String uuid;
        String name;
        long resolvedAtMs;
        long retryAfterMs;

        Entry(String uuid, String name, long resolvedAtMs, long retryAfterMs) {
            this.uuid = uuid; this.name = name; this.resolvedAtMs = resolvedAtMs; this.retryAfterMs = retryAfterMs;
        }
    }
}
