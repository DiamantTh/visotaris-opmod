package systems.diath.visotaris_opmod.cache;

import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionCategory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Separate local cache for the read-only OPSUCHT auction house. Never feeds MarketCache. */
public final class AuctionCache {
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<String, Auction> finalized = new LinkedHashMap<>();
    private volatile List<AuctionCategory> categories = List.of();
    private final AtomicLong lastUpdatedMs = new AtomicLong();
    private volatile String lastEventId;
    private static final java.util.Set<String> TERMINAL_STATES = java.util.Set.of(
        "ENDED", "SOLD", "CANCELLED", "EXPIRED", "INSTANT_BOUGHT"
    );

    public static boolean isTerminalState(String state) {
        return state != null && TERMINAL_STATES.contains(state.toUpperCase(java.util.Locale.ROOT));
    }

    public void replaceCategories(List<AuctionCategory> values) { categories = List.copyOf(values); }
    public List<AuctionCategory> categories() { return categories; }
    public synchronized void replaceActive(Collection<Auction> values) {
        auctions.clear();
        for (Auction auction : values) if (auction != null && auction.uid() != null) {
            if (isTerminalState(auction.state())) {
                finalized.put(auction.uid(), auction);
                continue;
            }
            finalized.remove(auction.uid());
            auctions.put(auction.uid(), auction);
        }
        while (finalized.size() > 500) finalized.remove(finalized.keySet().iterator().next());
        lastUpdatedMs.set(System.currentTimeMillis());
    }
    public synchronized void upsert(Auction auction) {
        if (auction != null && auction.uid() != null) {
            finalized.remove(auction.uid());
            auctions.put(auction.uid(), auction);
            lastUpdatedMs.set(System.currentTimeMillis());
        }
    }
    public void remove(String uid) { if (uid != null) { auctions.remove(uid); lastUpdatedMs.set(System.currentTimeMillis()); } }
    public synchronized void finish(Auction auction) {
        if (auction == null || auction.uid() == null) return;
        auctions.remove(auction.uid());
        finalized.put(auction.uid(), auction);
        if (finalized.size() > 500) {
            finalized.remove(finalized.keySet().iterator().next());
        }
        lastUpdatedMs.set(System.currentTimeMillis());
    }
    public Auction get(String uid) { return uid == null ? null : auctions.get(uid); }
    public Map<String, Auction> snapshot() { return Map.copyOf(auctions); }
    public synchronized Map<String, Auction> finalizedSnapshot() { return Map.copyOf(finalized); }
    public long getLastUpdatedMs() { return lastUpdatedMs.get(); }
    public long getAgeSeconds() { long ts = lastUpdatedMs.get(); return ts == 0 ? Long.MAX_VALUE : (System.currentTimeMillis() - ts) / 1000; }
    public String getLastEventId() { return lastEventId; }
    public void setLastEventId(String value) { if (value != null && !value.isBlank()) lastEventId = value; }
    public synchronized void restore(Collection<Auction> active, Collection<AuctionCategory> storedCategories,
                                     Collection<Auction> storedFinalized, long updatedAtMs, String eventId) {
        auctions.clear();
        if (active != null) for (Auction auction : active)
            if (auction != null && auction.uid() != null) auctions.put(auction.uid(), auction);
        finalized.clear();
        if (storedFinalized != null) for (Auction auction : storedFinalized)
            if (auction != null && auction.uid() != null) finalized.put(auction.uid(), auction);
        while (finalized.size() > 500) finalized.remove(finalized.keySet().iterator().next());
        categories = storedCategories == null ? List.of() : List.copyOf(storedCategories);
        lastUpdatedMs.set(Math.max(0, updatedAtMs));
        lastEventId = eventId == null || eventId.isBlank() ? null : eventId;
    }
}
