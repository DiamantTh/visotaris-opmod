package systems.diath.visotaris_opmod.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Distinguishes regular registry items from OPSUCHT-specific variants sharing a
 * vanilla carrier material. Enchantments, damage and ordinary stack components
 * intentionally do not make an item custom; a custom name, item name, lore or
 * custom model data does.
 */
public final class StackClassification {
    private StackClassification() { }

    public static boolean isCustomVariant(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME)
            || stack.has(DataComponents.ITEM_NAME)
            || stack.has(DataComponents.LORE)
            || stack.has(DataComponents.CUSTOM_MODEL_DATA);
    }
}
