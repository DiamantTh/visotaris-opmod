package systems.diath.visotaris_opmod.services;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import systems.diath.visotaris_opmod.cache.MarketCache;
import systems.diath.visotaris_opmod.cache.ShardCache;
import systems.diath.visotaris_opmod.config.ConfigManager;
import systems.diath.visotaris_opmod.model.InventoryValuation;
import systems.diath.visotaris_opmod.model.MarketPrice;
import systems.diath.visotaris_opmod.model.MerchantCurrency;
import systems.diath.visotaris_opmod.model.ShardRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Berechnet den Gesamtwert eines Inventars oder Containers.
 *
 * Shulker-Inhalte werden rekursiv mitgerechnet (via {@code DataComponents.CONTAINER}).
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
        double redcoins = 0;
        boolean hasShards   = false;
        boolean hasRedcoins = false;
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
                    InventoryValuation inner = evaluate(contents.nonEmptyItems());
                    buy  += inner.getBuyTotal();
                    sell += inner.getSellTotal();
                    shard += inner.getShardTotal();
                    redcoins += inner.getRedcoinTotal();
                    hasShards |= inner.hasShards();
                    hasRedcoins |= inner.hasRedcoins();
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

            Optional<ShardRate> merchantRate = shardCache.get(merchantKey(stack, key));
            if (merchantRate.isPresent()) {
                double total = merchantRate.get().getExchangeRate() * stack.getCount();
                MerchantCurrency currency = MerchantCurrency.fromApiTarget(merchantRate.get().getTarget());
                if (currency == MerchantCurrency.OPSHARDS) {
                    shard += total;
                    hasShards = true;
                } else if (currency == MerchantCurrency.REDCOINS) {
                    redcoins += total;
                    hasRedcoins = true;
                }
            }
        }

        return new InventoryValuation(buy, sell, shard, redcoins, hasShards, hasRedcoins, hasShulkers);
    }

    private String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    /** Ergänzt den Key für OPSUCHT-Custom-Items (z.B. paper#626). */
    private static String merchantKey(ItemStack stack, String baseKey) {
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null && !cmd.floats().isEmpty()) {
            return baseKey + "#" + (int) cmd.floats().get(0).floatValue();
        }
        return baseKey;
    }
}
