package systems.diath.noopmod.services;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import systems.diath.noopmod.cache.MarketCache;
import systems.diath.noopmod.cache.ShardCache;
import systems.diath.noopmod.config.ConfigManager;
import systems.diath.noopmod.model.MarketPrice;
import systems.diath.noopmod.model.ShardRate;

import java.util.List;
import java.util.Optional;

/**
 * Fügt Marktpreis- und Shard-Informationen zu Item-Tooltips hinzu.
 * Geschieht auf dem Render-Thread; kein Netzwerk, kein Blocking.
 *
 * Item-Schlüssel:
 *   - Vanilla-Items: Registry-Pfad (z.B. "acacia_leaves") – passt direkt auf MarketCache-Keys.
 *   - Custom-Model-Data-Items: Basis-Key + "#" + CMD-Wert (z.B. "paper#626").
 *     Damit können OPSUCHT-spezifische Paper-Items als Shard-Source erkannt werden.
 */
public final class TooltipValueService {

    private final MarketCache   marketCache;
    private final ShardCache    shardCache;
    private final ConfigManager config;

    public TooltipValueService(MarketCache marketCache, ShardCache shardCache, ConfigManager config) {
        this.marketCache = marketCache;
        this.shardCache  = shardCache;
        this.config      = config;
    }

    /**
     * Hängt Preis-Infos an die gegebene Tooltip-Liste.
     * Wird direkt im ItemTooltipCallback auf dem Client-Thread aufgerufen.
     */
    public void appendTooltips(ItemStack stack, List<Text> lines) {
        if (stack.isEmpty()) return;

        String baseKey = resolveBaseKey(stack);

        // Marktpreis
        Optional<MarketPrice> price = marketCache.get(baseKey);
        price.ifPresent(p -> {
            lines.add(Text.literal("§8[Visotaris OPMod]"));
            if (p.getBuy()  > 0) lines.add(Text.literal("§eKaufpreis:      §f" + formatMoney(p.getBuy())));
            if (p.getSell() > 0) lines.add(Text.literal("§eVerkaufspreis:  §f" + formatMoney(p.getSell())));
        });

        // Shardkurs: erst einfaches Lookup, dann mit custom_model_data
        Optional<ShardRate> shard = findShard(stack, baseKey);
        shard.ifPresent(s ->
            lines.add(Text.literal("§bShardkurs: §f" + s.getExchangeRate() + " OPS"))
        );
    }

    // ── Item-Key-Ableitung ────────────────────────────────────────────────────

    /**
     * Gibt den Basis-Registrierungs-Pfad des Items zurück (ohne Namespace).
     * z.B. {@code minecraft:acacia_leaves} → {@code acacia_leaves}
     */
    private static String resolveBaseKey(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath();
    }

    /**
     * Sucht nach einem Shardkurs für den gegebenen ItemStack.
     *
     * Einfache Shard-Items (diamond_block, netherite_ingot) werden direkt per
     * Registry-Pfad gefunden.
     *
     * Komplexe OPSUCHT-Items (Gräbergemisch, Holzbündel) sind paper-Items mit
     * custom_model_data. Ab 1.21.4 liegt der CMD-Wert als floats().get(0) im
     * CustomModelDataComponent. Daraus wird "paper#626" gebildet, was dem Key
     * im ShardCache (aus MerchantApiClient.normalizeSource) entspricht.
     */
    private Optional<ShardRate> findShard(ItemStack stack, String baseKey) {
        // Direktes Lookup (funktioniert für diamond_block, netherite_ingot etc.)
        Optional<ShardRate> direct = shardCache.get(baseKey);
        if (direct.isPresent()) return direct;

        // CMD-basiertes Lookup für OPSUCHT-Paper-Items (Gräbergemisch = paper#626 etc.)
        CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd != null && !cmd.floats().isEmpty()) {
            int cmdValue = (int) cmd.floats().get(0).floatValue();
            String cmdKey = baseKey + "#" + cmdValue;
            return shardCache.get(cmdKey);
        }

        return Optional.empty();
    }

    private static String formatMoney(double value) {
        if (value <= 0) return "–";
        return String.format("%,.2f $", value);
    }
}