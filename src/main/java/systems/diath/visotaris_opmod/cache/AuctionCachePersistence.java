package systems.diath.visotaris_opmod.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import systems.diath.visotaris_opmod.VisotarisConst;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.api.AuctionApiClient;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionCategory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Disk snapshot for the shared auction cache; it is not an independent data store. */
public final class AuctionCachePersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    public AuctionCachePersistence() {
        this(VisotarisConst.getCacheDir("auctions").toPath().resolve("snapshot.json"));
    }

    AuctionCachePersistence(Path file) { this.file = file; }

    public void loadInto(AuctionCache cache) {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Auction> active = readAuctions(root.getAsJsonArray("active"));
            List<Auction> finals = readAuctions(root.getAsJsonArray("finalized"));
            List<AuctionCategory> categories = root.has("categories")
                ? GSON.fromJson(root.get("categories"), new com.google.gson.reflect.TypeToken<List<AuctionCategory>>() { }.getType())
                : List.of();
            long updated = root.has("updatedAtMs") ? root.get("updatedAtMs").getAsLong() : 0;
            String lastEventId = root.has("lastEventId") && !root.get("lastEventId").isJsonNull()
                ? root.get("lastEventId").getAsString() : null;
            cache.restore(active, categories, finals, updated, lastEventId);
        } catch (Exception e) {
            VisotarisLogger.warn("Auction-Cache konnte nicht geladen werden: {}", e.getMessage());
        }
    }

    public synchronized void save(AuctionCache cache) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.add("active", auctionArray(cache.snapshot().values()));
            root.add("finalized", auctionArray(cache.finalizedSnapshot().values()));
            root.add("categories", GSON.toJsonTree(cache.categories()));
            root.addProperty("updatedAtMs", cache.getLastUpdatedMs());
            if (cache.getLastEventId() == null) root.add("lastEventId", null);
            else root.addProperty("lastEventId", cache.getLastEventId());
            Path temporary = Files.createTempFile(file.getParent(), "auctions-", ".json.tmp");
            try {
                Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException unsupportedAtomicMove) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException e) {
            VisotarisLogger.warn("Auction-Cache konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }

    private static JsonArray auctionArray(Iterable<Auction> auctions) {
        JsonArray values = new JsonArray();
        for (Auction auction : auctions) values.add(AuctionApiClient.toJsonElement(auction));
        return values;
    }

    private static List<Auction> readAuctions(JsonArray values) {
        if (values == null) return List.of();
        List<Auction> result = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            Auction auction = AuctionApiClient.parseAuction(value);
            if (auction != null && auction.uid() != null) result.add(auction);
        }
        return result;
    }
}
