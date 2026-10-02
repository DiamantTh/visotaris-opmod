package systems.diath.visotaris_opmod.cache;

import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionCategory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Separate local cache for the read-only OPSUCHT auction house. Never feeds MarketCache. */
public final class AuctionCache {
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private volatile List<AuctionCategory> categories = List.of();
    private final AtomicLong lastUpdatedMs = new AtomicLong();
    private volatile String lastEventId;

    public void replaceCategories(List<AuctionCategory> values) { categories = List.copyOf(values); }
    public List<AuctionCategory> categories() { return categories; }
    public void replaceActive(Collection<Auction> values) {
        auctions.clear();
        for (Auction auction : values) if (auction != null && auction.uid() != null) auctions.put(auction.uid(), auction);
        lastUpdatedMs.set(System.currentTimeMillis());
    }
    public void upsert(Auction auction) {
        if (auction != null && auction.uid() != null) { auctions.put(auction.uid(), auction); lastUpdatedMs.set(System.currentTimeMillis()); }
    }
    public void remove(String uid) { if (uid != null) { auctions.remove(uid); lastUpdatedMs.set(System.currentTimeMillis()); } }
    public Map<String, Auction> snapshot() { return Map.copyOf(auctions); }
    public long getLastUpdatedMs() { return lastUpdatedMs.get(); }
    public long getAgeSeconds() { long ts = lastUpdatedMs.get(); return ts == 0 ? Long.MAX_VALUE : (System.currentTimeMillis() - ts) / 1000; }
    public String getLastEventId() { return lastEventId; }
    public void setLastEventId(String value) { if (value != null && !value.isBlank()) lastEventId = value; }
}
