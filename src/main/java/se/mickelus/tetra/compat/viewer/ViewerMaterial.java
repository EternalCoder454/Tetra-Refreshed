package se.mickelus.tetra.compat.viewer;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.module.data.MaterialData;

import java.util.List;

/**
 * One material, as a recipe viewer wants to see it: a name, the items that count as it, and the
 * stats it contributes.
 *
 * This carries no viewer types on purpose. A plugin for any viewer reads these and renders them its
 * own way, which is what keeps the extraction in one place rather than once per viewer.
 *
 * @param key      the material key, e.g. iron
 * @param category the material category, e.g. metal
 * @param items    every item that counts as this material, already resolved from tags
 * @param data     the material itself, for the stats a viewer chooses to show
 */
public record ViewerMaterial(String key, String category, List<ItemStack> items, MaterialData data) {
}
