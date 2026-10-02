package systems.diath.visotaris_opmod.services;

import com.google.gson.JsonElement;
import okhttp3.Response;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.AuctionApiClient;
import systems.diath.visotaris_opmod.cache.AuctionCache;
import systems.diath.visotaris_opmod.config.ConfigManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Initial auction snapshot plus a reconnecting SSE consumer. No Minecraft action is sent. */
public final class AuctionSyncService {
    private final AuctionCache cache;
    private final AuctionApiClient api;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "visotaris-auction-sync"); t.setDaemon(true); return t; });
    private volatile boolean running;
    private int reconnectSeconds = 2;
    public AuctionSyncService(AuctionCache cache, ConfigManager config) { this.cache = cache; this.api = new AuctionApiClient(config); }
    public void start() { running = true; executor.execute(this::initialAndStream); }
    public void stop() { running = false; executor.shutdownNow(); }
    public void refreshSnapshot() { executor.execute(this::reloadActive); }
    private void initialAndStream() {
        try { cache.replaceCategories(api.fetchCategories()); }
        catch (IOException e) { VisotarisLogger.warn("Auction-API Kategorien nicht erreichbar: {}", e.getMessage()); }
        reloadActive();
        consumeLoop();
    }
    private void reloadActive() {
        try { cache.replaceActive(api.fetchActive()); } catch (IOException e) { VisotarisLogger.warn("Auction-API active nicht erreichbar: {}", e.getMessage()); }
    }
    private void consumeLoop() {
        while (running) {
            try (Response response = api.openStream(cache.getLastEventId()); BufferedReader reader = new BufferedReader(response.body().charStream())) {
                reconnectSeconds = 2;
                String id = null, event = null; StringBuilder data = new StringBuilder(); String line;
                while (running && (line = reader.readLine()) != null) {
                    if (line.isEmpty()) { dispatch(id, event, data.toString()); id = event = null; data.setLength(0); continue; }
                    if (line.startsWith("id:")) id = line.substring(3).trim();
                    else if (line.startsWith("event:")) event = line.substring(6).trim();
                    else if (line.startsWith("data:")) { if (!data.isEmpty()) data.append('\n'); data.append(line.substring(5).trim()); }
                }
            } catch (IOException e) { if (running) VisotarisLogger.warn("Auction-SSE getrennt: {}", e.getMessage()); }
            if (running) { try { TimeUnit.SECONDS.sleep(reconnectSeconds); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } reconnectSeconds = Math.min(60, reconnectSeconds * 2); }
        }
    }
    private void dispatch(String id, String event, String data) {
        if (id != null) cache.setLastEventId(id);
        if (event == null) return;
        if ("stream.reset".equals(event)) { reloadActive(); return; }
        try {
            JsonElement payload = api.parseElement(data);
            if ("auction.removed".equals(event)) {
                String uid = extractUid(payload);
                cache.remove(uid); return;
            }
            if (event.startsWith("auction.")) {
                var auction = api.parseAuction(payload);
                if (auction != null && auction.uid() != null && !auction.uid().isBlank()) cache.upsert(auction);
            }
        } catch (Exception e) { VisotarisLogger.warn("Auction-SSE Event {} konnte nicht verarbeitet werden: {}", event, e.getMessage()); }
    }

    private static String extractUid(JsonElement payload) {
        if (payload == null || payload.isJsonNull()) return null;
        if (payload.isJsonPrimitive()) return payload.getAsString();
        if (!payload.isJsonObject()) return null;
        var object = payload.getAsJsonObject();
        if (object.has("uid")) return object.get("uid").getAsString();
        if (object.has("auction") && object.get("auction").isJsonObject() && object.getAsJsonObject("auction").has("uid"))
            return object.getAsJsonObject("auction").get("uid").getAsString();
        return null;
    }
}
