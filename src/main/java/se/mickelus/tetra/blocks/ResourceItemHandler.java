package se.mickelus.tetra.blocks;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * A modifiable item handler view over a resource handler.
 *
 * IItemHandler.of gives back a read and transfer view alone, and SlotItemHandler.set casts its
 * handler to IItemHandlerModifiable, so a menu built on that adapter died with a ClassCastException
 * the moment the server sent its contents. The read half still delegates to the adapter; the write
 * half goes to the resource handler, which is the only place a plain set lives.
 */
public class ResourceItemHandler implements IItemHandlerModifiable {
    private final ItemStacksResourceHandler handler;
    private final IItemHandler view;

    public ResourceItemHandler(ItemStacksResourceHandler handler) {
        this.handler = handler;
        this.view = IItemHandler.of(handler);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack itemStack) {
        handler.set(slot, ItemResource.of(itemStack), itemStack.getCount());
    }

    @Override
    public int getSlots() {
        return view.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return view.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
        return view.insertItem(slot, itemStack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return view.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return view.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack) {
        return view.isItemValid(slot, itemStack);
    }
}
