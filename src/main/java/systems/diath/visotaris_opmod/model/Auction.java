package systems.diath.visotaris_opmod.model;

import java.time.Instant;
import java.util.Map;

/**
 * Read-only auction snapshot from api.opsucht.net/auctions.
 * A regular active snapshot includes both startTime and endTime; nullable values
 * remain supported defensively for incomplete or malformed payloads and events.
 */
public record Auction(String uid, String seller, AuctionItem item, String category, String state,
                      double startBid, Double instantBuyPrice, double currentBid,
                      String highestBidder, Map<String, Double> bids,
                      Instant startTime, Instant endTime) { }
