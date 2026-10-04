package systems.diath.visotaris_opmod.services;

import org.junit.jupiter.api.Test;
import systems.diath.visotaris_opmod.cache.AuctionCache;
import systems.diath.visotaris_opmod.model.Auction;

import static org.junit.jupiter.api.Assertions.*;

class AuctionEventProcessorTest {
    private static final String UID = "de27aa9bfd33440dfac2c6eee9bc371d";
    private static final String SNAPSHOT = """
        {"uid":"de27aa9bfd33440dfac2c6eee9bc371d","seller":"seller-uuid","item":{"material":"PAPER","icon":"https://cdn.opsucht.net/item.png","amount":2,"displayName":"Custom Item","lore":["Lore"],"enchantments":{"minecraft:sharpness":5}},"category":"misc","state":"ACTIVE","startBid":3.0,"instantBuyPrice":15.0,"currentBid":3.0,"highestBidder":null,"bids":{},"startTime":"2026-10-03T10:00:00Z","endTime":"2026-10-03T11:00:00Z"}
        """;

    @Test void createdAndPartialBidUpdateUseUidAndReplaceEndTime() {
        AuctionCache cache = new AuctionCache();
        AuctionEventProcessor processor = new AuctionEventProcessor(cache);
        assertEquals(AuctionEventProcessor.Result.UPDATED, processor.apply("auction.created", SNAPSHOT));
        assertTrue(cache.snapshot().containsKey(UID));
        assertEquals(2, cache.get(UID).item().amount());

        String later = """
            {"uid":"de27aa9bfd33440dfac2c6eee9bc371d","auction":{"uid":"de27aa9bfd33440dfac2c6eee9bc371d","currentBid":5.0,"highestBidder":"bidder-uuid","bids":{"bidder-uuid":5.0},"endTime":"2026-10-03T11:10:00Z"}}
            """;
        assertEquals(AuctionEventProcessor.Result.UPDATED, processor.apply("auction.bid_placed", later));
        Auction updated = cache.get(UID);
        assertEquals(5.0, updated.currentBid());
        assertEquals("bidder-uuid", updated.highestBidder());
        assertEquals(2, updated.item().amount(), "omitted fields survive partial events");
        assertEquals("2026-10-03T11:10:00Z", updated.endTime().toString());
        assertEquals(1, updated.bids().size());
    }

    @Test void observerReceivesAuctionSellerAfterSnapshotAndStreamUpdates() {
        java.util.List<String> sellers = new java.util.ArrayList<>();
        AuctionEventProcessor processor = new AuctionEventProcessor(new AuctionCache(), auction -> sellers.add(auction.seller()));
        processor.apply("auction.created", SNAPSHOT);
        processor.apply("auction.updated", "{\"uid\":\"" + UID + "\",\"auction\":{\"uid\":\"" + UID + "\",\"currentBid\":4.0}}");
        assertEquals(java.util.List.of("seller-uuid", "seller-uuid"), sellers);
    }

    @Test void updatedEventReplacesDataForSameUid() {
        AuctionCache cache = new AuctionCache();
        AuctionEventProcessor processor = new AuctionEventProcessor(cache);
        processor.apply("auction.created", SNAPSHOT);
        assertEquals(AuctionEventProcessor.Result.UPDATED, processor.apply("auction.updated",
            "{\"uid\":\"" + UID + "\",\"auction\":{\"uid\":\"" + UID + "\",\"currentBid\":8.0}}"));
        assertEquals(8.0, cache.get(UID).currentBid());
        assertEquals(1, cache.snapshot().size());
    }

    @Test void stateTurningTerminalAlsoMovesAuctionOutOfActiveList() {
        AuctionCache cache = new AuctionCache();
        AuctionEventProcessor processor = new AuctionEventProcessor(cache);
        processor.apply("auction.created", SNAPSHOT);
        assertEquals(AuctionEventProcessor.Result.FINALIZED, processor.apply("auction.updated",
            "{\"uid\":\"" + UID + "\",\"auction\":{\"uid\":\"" + UID + "\",\"state\":\"ENDED\"}}"));
        assertFalse(cache.snapshot().containsKey(UID));
        assertEquals("ENDED", cache.finalizedSnapshot().get(UID).state());
    }

    @Test void removalAndTerminalEventsLeaveActiveCacheAndRetainFinalSnapshot() {
        for (String event : new String[]{"auction.sold", "auction.expired", "auction.cancelled", "auction.instant_bought"}) {
            AuctionCache cache = new AuctionCache();
            AuctionEventProcessor processor = new AuctionEventProcessor(cache);
            processor.apply("auction.created", SNAPSHOT);
            assertEquals(AuctionEventProcessor.Result.FINALIZED, processor.apply(event, SNAPSHOT));
            assertFalse(cache.snapshot().containsKey(UID), event + " removes the active auction");
            assertTrue(cache.finalizedSnapshot().containsKey(UID), event + " retains final details");
        }

        AuctionCache cache = new AuctionCache();
        AuctionEventProcessor processor = new AuctionEventProcessor(cache);
        processor.apply("auction.created", SNAPSHOT);
        assertEquals(AuctionEventProcessor.Result.REMOVED, processor.apply("auction.removed", "{\"uid\":\"" + UID + "\"}"));
        assertTrue(cache.snapshot().isEmpty());
    }

    @Test void resetRequestsSnapshotAndUnknownEventsAreIgnored() {
        AuctionEventProcessor processor = new AuctionEventProcessor(new AuctionCache());
        assertEquals(AuctionEventProcessor.Result.RESET, processor.apply("stream.reset", "{}"));
        assertEquals(AuctionEventProcessor.Result.IGNORED, processor.apply("auction.future", "{}"));
    }

    @Test void liveUpdatesRequireOptInUnlessDevEnvironmentExplicitlyEnablesThem() {
        assertFalse(AuctionSyncService.shouldEnableLiveUpdates(false, null));
        assertFalse(AuctionSyncService.shouldEnableLiveUpdates(false, "false"));
        assertTrue(AuctionSyncService.shouldEnableLiveUpdates(true, null));
        assertTrue(AuctionSyncService.shouldEnableLiveUpdates(false, "true"));
    }
}
