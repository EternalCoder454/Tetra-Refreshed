package se.mickelus.tetra.data.predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.util.RegistryHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
public class SimpleItemPredicate implements TetraItemPredicate {
    private final Set<Item> items;
    @Nullable
    private final TagKey<Item> tag;

    /**
     * Set when items were named and none of them exist.
     *
     * <p>Without this the predicate matched every item in the game. An empty item set means "do not
     * filter by item" and a null tag means "do not filter by tag", so a predicate that had been
     * asked for specific items, and resolved none of them, was left filtering by nothing at all and
     * said yes to everything.
     *
     * <p>That is not a hypothetical. Art of Forging names three materials from mods that need not be
     * installed. With none of them present, one was a fibre with 6.5 hardness, one was a metal with
     * 8.5 hardness and a netherite tool level, and both accepted any item that could be put in a
     * slot. A holosphere made a claw. Wood wrapped a handle. Hammer tiers came out of nowhere.
     *
     * <p>Naming an item from a mod that might not be installed is a normal thing for an addon to do,
     * and the right outcome is that the material is simply unobtainable, the way an effect nothing
     * registers contributes nothing.
     */
    private final boolean unobtainable;

    public SimpleItemPredicate(Collection<Identifier> itemIds, @Nullable Identifier tagId) {
        this.items = new HashSet<>();
        itemIds.stream()
                .map(itemId -> RegistryHelper.get(BuiltInRegistries.ITEM, itemId))
                .filter(item -> item != null)
                .forEach(items::add);
        this.tag = tagId != null ? ItemTags.create(tagId) : null;
        this.unobtainable = !itemIds.isEmpty() && items.isEmpty();
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (itemStack.isEmpty() || unobtainable) {
            return false;
        }

        if (!items.isEmpty() && !items.contains(itemStack.getItem())) {
            return false;
        }

        return tag == null || itemStack.is(tag);
    }
}
