package systems.diath.visotaris_opmod.model;

import java.time.Instant;
import java.util.Map;

/** Read-only active auction snapshot from api.opsucht.net/auctions. */
public record Auction(String uid, String seller, AuctionItem item, String category, String state,
                      double startBid, Double instantBuyPrice, double currentBid,
                      String highestBidder, Map<String, Double> bids,
                      Instant startTime, Instant endTime) { }
