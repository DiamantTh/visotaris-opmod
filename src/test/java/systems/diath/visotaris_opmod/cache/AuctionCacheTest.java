package systems.diath.visotaris_opmod.cache;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCacheTest {
    private static Auction auction(String uid, double bid) {
        return new Auction(uid, "seller", new AuctionItem("STONE", null, 1, "Test", List.of(), Map.of()),
            "custom_items", "ACTIVE", 1, null, bid, null, Map.of(), Instant.EPOCH, Instant.MAX);
    }

    @Test void snapshotUpdateAndRemovalStaySeparateFromMarketState() {
        AuctionCache cache = new AuctionCache();
        cache.replaceActive(List.of(auction("one", 10), auction("two", 20)));
        assertEquals(2, cache.snapshot().size());
        cache.upsert(auction("one", 25));
        assertEquals(25, cache.snapshot().get("one").currentBid());
        cache.remove("two");
        assertEquals(1, cache.snapshot().size());
        cache.setLastEventId("event-42");
        assertEquals("event-42", cache.getLastEventId());
    }
}
