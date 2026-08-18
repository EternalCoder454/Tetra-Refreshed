package se.mickelus.tetra.blocks;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

public interface ItemHandlerBlockEntity {
    /**
     * The item capability carries a ResourceHandler now rather than an IItemHandler, so the two
     * views are declared side by side. Tetra's own code reads through the IItemHandler view, which
     * still has the slot shaped api the menus and renderers were written against.
     */
    ResourceHandler<ItemResource> getResourceHandler(@Nullable Direction side);

    IItemHandler getItemHandler(@Nullable Direction side);
}
