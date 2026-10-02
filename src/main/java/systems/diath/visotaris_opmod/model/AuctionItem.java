package systems.diath.visotaris_opmod.model;

import java.util.List;
import java.util.Map;

/** Item snapshot embedded in an auction. It is intentionally not a Minecraft ItemStack. */
public record AuctionItem(String material, String icon, int amount, String displayName,
                          List<String> lore, Map<String, Integer> enchantments) { }
