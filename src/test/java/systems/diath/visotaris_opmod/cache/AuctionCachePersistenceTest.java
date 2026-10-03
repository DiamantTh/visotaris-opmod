package systems.diath.visotaris_opmod.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import systems.diath.visotaris_opmod.model.Auction;
import systems.diath.visotaris_opmod.model.AuctionCategory;
import systems.diath.visotaris_opmod.model.AuctionItem;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCachePersistenceTest {
    @TempDir Path directory;

    @Test void persistsSharedActiveAndFinalSnapshotsCategoriesAndResumeId() {
        Path file = directory.resolve("auction-cache.json");
        Auction active = auction("active-uid", "ACTIVE", 25);
        Auction sold = auction("sold-uid", "SOLD", 42);
        AuctionCache original = new AuctionCache();
        original.replaceCategories(List.of(new AuctionCategory("misc", "Sonstiges", "PAPER", null, null, List.of("PAPER"))));
        original.replaceActive(List.of(active));
        original.finish(sold);
        original.setLastEventId("event-99");
        long updatedAt = original.getLastUpdatedMs();

        AuctionCachePersistence persistence = new AuctionCachePersistence(file);
        persistence.save(original);
        AuctionCache restored = new AuctionCache();
        persistence.loadInto(restored);

        assertEquals(Map.of("active-uid", active), restored.snapshot());
        assertEquals(Map.of("sold-uid", sold), restored.finalizedSnapshot());
        assertEquals("event-99", restored.getLastEventId());
        assertEquals(updatedAt, restored.getLastUpdatedMs());
        assertEquals("misc", restored.categories().getFirst().name());
    }

    private static Auction auction(String uid, String state, double bid) {
        AuctionItem item = new AuctionItem("PAPER", "https://cdn.opsucht.net/item.png", 2,
            "Custom Item", List.of("Lore"), Map.of("minecraft:sharpness", 5));
        return new Auction(uid, "seller-uuid", item, "misc", state, 5, 50.0, bid, "bidder-uuid",
            Map.of("bidder-uuid", bid), Instant.parse("2026-10-03T10:00:00Z"), Instant.parse("2026-10-03T11:00:00Z"));
    }
}
