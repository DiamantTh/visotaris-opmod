package systems.diath.visotaris_opmod.api;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuctionApiClientContractTest {

    @Test
    void readsActiveAuctionFieldsWithoutFallingBackToMarketValues() {
        var auction = AuctionApiClient.parseAuction(JsonParser.parseString("""
            {"uid":"auction-1","seller":"Visotaris","item":{"material":"PAPER","icon":"paper","amount":3,"displayName":"Gräbergemisch","lore":["OPShards"],"enchantments":{}},"category":"opshards","state":"ACTIVE","startBid":10.0,"instantBuyPrice":null,"currentBid":21.56,"highestBidder":null,"bids":{},"startTime":"2026-10-03T12:00:00Z","endTime":"2026-10-03T13:00:00Z"}
            """));

        assertEquals("auction-1", auction.uid());
        assertEquals("PAPER", auction.item().material());
        assertEquals("Gräbergemisch", auction.item().displayName());
        assertEquals(21.56, auction.currentBid());
        assertNull(auction.instantBuyPrice());
        assertEquals(Instant.parse("2026-10-03T13:00:00Z"), auction.endTime());
    }

    @Test
    void acceptsDocumentedEventWrapper() {
        var auction = AuctionApiClient.parseAuction(JsonParser.parseString("""
            {"auction":{"uid":"auction-2","item":{"material":"STONE","amount":1,"displayName":"Stein","lore":[],"enchantments":{}},"currentBid":4,"bids":{}}}
            """));
        assertEquals("auction-2", auction.uid());
        assertEquals("Stein", auction.item().displayName());
    }
}
