package systems.diath.visotaris_opmod.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import systems.diath.visotaris_opmod.api.AuctionApiClient;
import systems.diath.visotaris_opmod.cache.AuctionCache;
import systems.diath.visotaris_opmod.model.Auction;

import java.util.function.Consumer;

/** Applies public auction events to the single UID-keyed local cache. */
public final class AuctionEventProcessor {
    public enum Result { IGNORED, UPDATED, REMOVED, FINALIZED, RESET }

    private static final java.util.Set<String> EVENTS = java.util.Set.of(
        "auction.created", "auction.bid_placed", "auction.updated", "auction.instant_bought",
        "auction.sold", "auction.expired", "auction.cancelled", "auction.removed", "stream.reset"
    );
    private static final java.util.Set<String> TERMINAL_EVENTS = java.util.Set.of(
        "auction.instant_bought", "auction.sold", "auction.expired", "auction.cancelled"
    );

    private final AuctionCache cache;
    private final Consumer<Auction> auctionObserved;

    public AuctionEventProcessor(AuctionCache cache) { this(cache, ignored -> { }); }

    public AuctionEventProcessor(AuctionCache cache, Consumer<Auction> auctionObserved) {
        this.cache = cache;
        this.auctionObserved = auctionObserved == null ? ignored -> { } : auctionObserved;
    }

    public Result apply(String event, String data) {
        if (!EVENTS.contains(event)) return Result.IGNORED;
        if ("stream.reset".equals(event)) return Result.RESET;
        try {
            JsonElement envelope = AuctionApiClient.parseElement(data);
            if ("auction.removed".equals(event)) {
                String uid = uidFrom(envelope);
                if (uid == null) return Result.IGNORED;
                cache.remove(uid);
                return Result.REMOVED;
            }

            JsonObject update = auctionObject(envelope);
            String uid = uidFrom(envelope);
            if (uid == null) uid = uidFrom(update);
            if (uid == null || uid.isBlank()) return Result.IGNORED;
            update.addProperty("uid", uid);

            Auction previous = cache.get(uid);
            JsonObject full = previous == null ? new JsonObject() : AuctionApiClient.toJsonElement(previous).getAsJsonObject();
            merge(full, update);
            Auction auction = AuctionApiClient.parseAuction(full);
            if (auction == null || auction.uid() == null || auction.uid().isBlank()) return Result.IGNORED;
            if (TERMINAL_EVENTS.contains(event) || AuctionCache.isTerminalState(auction.state())) {
                cache.finish(auction);
                notifyObserved(auction);
                return Result.FINALIZED;
            }
            cache.upsert(auction);
            notifyObserved(auction);
            return Result.UPDATED;
        } catch (RuntimeException invalidPayload) {
            return Result.IGNORED;
        }
    }

    private void notifyObserved(Auction auction) {
        try { auctionObserved.accept(auction); }
        catch (RuntimeException ignored) { /* Profile prefetch must never invalidate an auction update. */ }
    }

    private static JsonObject auctionObject(JsonElement envelope) {
        if (envelope != null && envelope.isJsonObject()) {
            JsonElement auction = envelope.getAsJsonObject().get("auction");
            if (auction != null && auction.isJsonObject()) return auction.getAsJsonObject().deepCopy();
            return envelope.getAsJsonObject().deepCopy();
        }
        return new JsonObject();
    }

    private static String uidFrom(JsonElement value) {
        if (value == null || !value.isJsonObject()) return null;
        JsonElement uid = value.getAsJsonObject().get("uid");
        return uid != null && uid.isJsonPrimitive() ? uid.getAsString() : null;
    }

    /** Deeply overlays an event snapshot without dropping fields omitted by partial events. */
    private static void merge(JsonObject target, JsonObject overlay) {
        overlay.entrySet().forEach(entry -> {
            JsonElement oldValue = target.get(entry.getKey());
            JsonElement newValue = entry.getValue();
            if (oldValue != null && oldValue.isJsonObject() && newValue != null && newValue.isJsonObject()) {
                merge(oldValue.getAsJsonObject(), newValue.getAsJsonObject());
            } else {
                target.add(entry.getKey(), newValue == null ? null : newValue.deepCopy());
            }
        });
    }
}
