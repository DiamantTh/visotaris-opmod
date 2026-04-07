package systems.diath.visotaris.services;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import systems.diath.visotaris.cache.MarketCache;
import systems.diath.visotaris.cache.ShardCache;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.InventoryValuation;
import systems.diath.visotaris.model.MarketPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Berechnet den Gesamtwert eines Inventars oder Containers.
 *
 * Shulker-Inhalte werden rekursiv mitgerechnet (via {@code DataComponentTypes.CONTAINER}).
 * Der Aufruf darf vom Render-Thread erfolgen (kein Netzwerk, kein Blocking).
 */
public final class InventoryValuationService {

    private final MarketCache   marketCache;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return InventoryValuation.empty();

        var inv = mc.player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) stacks.add(inv.getStack(i));
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
                ContainerComponent contents = stack.get(DataComponentTypes.CONTAINER);
                if (contents != null) {
                    hasShulkers = true;
                    InventoryValuation inner = evaluate(contents.iterateNonEmpty());
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
        return Registries.ITEM.getId(stack.getItem()).getPath();
    }
}
