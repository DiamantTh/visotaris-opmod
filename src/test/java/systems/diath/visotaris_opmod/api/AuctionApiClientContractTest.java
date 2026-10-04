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
    void mapsTheRealOpsuchtCustomItemContractWithoutUsingItsCarrierAsName() {
        var auction = AuctionApiClient.parseAuction(JsonParser.parseString("""
            {"uid":"107840b1b5eff52ddda93ae07eeaf714","seller":"de27aa9bfd33440dfac2c6eee9bc371d","item":{"material":"GOLDEN_HORSE_ARMOR","icon":"https://items.opsucht.net/J3j9Ss0K1JL6.png","amount":1,"displayName":"Plüsch thalie","lore":["","Signiert von thalie am 01.02.2026"],"enchantments":{}},"category":"custom_items","state":"ACTIVE","startBid":1.0,"instantBuyPrice":null,"currentBid":1.0,"highestBidder":null,"bids":{},"startTime":"2026-10-04T10:00:00Z","endTime":"2026-10-04T11:00:00Z"}
            """));

        assertEquals("Plüsch thalie", auction.item().displayName());
        assertEquals("https://items.opsucht.net/J3j9Ss0K1JL6.png", auction.item().icon());
        assertEquals("GOLDEN_HORSE_ARMOR", auction.item().material());
        assertEquals(1, auction.item().amount());
        assertEquals(java.util.List.of("", "Signiert von thalie am 01.02.2026"), auction.item().lore());
        assertEquals(java.util.Map.of(), auction.item().enchantments());
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
