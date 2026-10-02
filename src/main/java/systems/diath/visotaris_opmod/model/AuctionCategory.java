package systems.diath.visotaris_opmod.model;

import java.util.List;

/** Read-only category metadata supplied by the public OPSUCHT auction API. */
public record AuctionCategory(String name, String displayName, String displayMaterial,
                              String icon, String parentCategory, List<String> matchTypes) { }
