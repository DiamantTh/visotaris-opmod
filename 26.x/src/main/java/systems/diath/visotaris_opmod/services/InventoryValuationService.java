package systems.diath.visotaris_opmod.services;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.cache.ShardCache;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.InventoryValuation;
import systems.diath.visotaris_opmod.model.MarketPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Berechnet den Gesamtwert eines Inventars oder Containers.
 *
 * Shulker-Inhalte werden rekursiv mitgerechnet (via {@code DataComponents.CONTAINER}).
 * Der Aufruf darf vom Render-Thread erfolgen (kein Netzwerk, kein Blocking).
 *
 * MC 26.x: {@code ItemContainerContents.nonEmptyItems()} liefert seit 26.2
 * {@code Iterable<ItemStackTemplate>} statt {@code Iterable<ItemStack>} (Mojang hat
 * Container-Inhalte von echten ItemStacks entkoppelt). Für die Bewertung reicht
 * {@code nonEmptyItemCopyStream()}, das direkt einen {@code Stream<ItemStack>} liefert.
 */
public final class InventoryValuationService {

    private final MarketCache   marketCache;
    @SuppressWarnings("unused") // TODO: Shard-Bewertung (siehe evaluate())
    private final ShardCache    shardCache;
    private final ConfigManager config;

    public InventoryValuationService(MarketCache marketCache, ShardCache shardCache,
                                     ConfigManager config) {
        this.marketCache = marketCache;
        this.shardCache  = shardCache;
        this.config      = config;
    }

    /**
     * Bewertet das aktuelle Spielerinventar.
     * Muss auf dem Client-Thread aufgerufen werden.
     */
    public InventoryValuation evaluatePlayerInventory() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return InventoryValuation.empty();

        var inv = mc.player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) stacks.add(inv.getItem(i));
        return evaluate(stacks);
    }

    /**
     * Bewertet eine beliebige Liste von ItemStacks (z.B. Container-Inhalt).
     */
    public InventoryValuation evaluate(Iterable<ItemStack> stacks) {
        double buy   = 0;
        double sell  = 0;
        double shard = 0;
        boolean hasShards   = false;
        boolean hasShulkers = false;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            // Shulker-Rekursion: Inhalt des Shulker-Box-Items bewerten
            if (config.getConfig().shulkerRecursion
                    && stack.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof ShulkerBoxBlock) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    hasShulkers = true;
                    InventoryValuation inner = evaluate(contents.nonEmptyItemCopyStream().toList());
                    buy  += inner.getBuyTotal();
                    sell += inner.getSellTotal();
                    continue;
                }
            }

            String key = itemKey(stack);
            Optional<MarketPrice> price = marketCache.get(key);

            if (price.isPresent()) {
                int count = stack.getCount();
                buy  += price.get().getBuy()  * count;
                sell += price.get().getSell() * count;
            }

            // TODO: Shard-Wert für shard-fähige Items addieren
        }

        return new InventoryValuation(buy, sell, shard, hasShards, hasShulkers);
    }

    private String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }
}
