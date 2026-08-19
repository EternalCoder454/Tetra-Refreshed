package se.mickelus.tetra.blocks;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

public interface ItemHandlerBlockEntity {
    /**
     * What the item capability carries, and what everything here reads.
     *
     * <p>There used to be a second IItemHandler view beside this one, because the menus and
     * renderers were written against the slot shaped api and a writable adapter had to be kept to
     * feed them. mutil's slot and container wrapper take a resource handler directly now, so the
     * handler that owns the items is the only view there is.
     */
    ResourceHandler<ItemResource> getResourceHandler(@Nullable Direction side);

    /**
     * How a menu writes a slot back.
     *
     * <p>A resource handler can insert and extract but cannot set a slot to an arbitrary stack,
     * which is what a player dropping an item into a slot does. The handlers here all store slots
     * and offer {@code set(int, resource, amount)}, which is exactly this interface, so each one
     * hands back a reference to its own method.
     */
    IndexModifier<ItemResource> getIndexModifier(@Nullable Direction side);
}
