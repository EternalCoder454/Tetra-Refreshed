package se.mickelus.tetra.blocks.forged.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.gui.ToggleableSlot;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class ForgedContainerMenu extends AbstractContainerMenu {
    public static Supplier<MenuType<ForgedContainerMenu>> type;

    private final ForgedContainerBlockEntity tile;

    private ToggleableSlot[][] compartmentSlots;
    private int currentCompartment = 0;

    public ForgedContainerMenu(int windowId, ForgedContainerBlockEntity tile, Container playerInventory, @Nullable Player player) {
        super(ForgedContainerMenu.type.get(), windowId);
        this.tile = tile;

        // material inventory
        var handler = tile.getResourceHandler(null);
        if (handler != null) {
            compartmentSlots = new ToggleableSlot[ForgedContainerBlockEntity.compartmentCount][];
            for (int i = 0; i < compartmentSlots.length; i++) {
                compartmentSlots[i] = new ToggleableSlot[ForgedContainerBlockEntity.compartmentSize];
                int offset = i * ForgedContainerBlockEntity.compartmentSize;
                for (int j = 0; j < 6; j++) {
                    for (int k = 0; k < 9; k++) {
                        int index = j * 9 + k;
                        compartmentSlots[i][index] = new ToggleableSlot(handler, tile.getIndexModifier(null), index + offset, k * 17 + 12, j * 17);
                        compartmentSlots[i][index].toggle(i == 0);
                        addSlot(compartmentSlots[i][index]);
                    }
                }
            }
        }

        // player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, i * 9 + j + 9, j * 17 + 12, i * 17 + 116));
            }
        }

        // player toolbar
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, i * 17 + 12, 171));
        }
    }

    public void changeCompartment(int compartmentIndex) {
        currentCompartment = compartmentIndex;
        for (int i = 0; i < compartmentSlots.length; i++) {
            boolean enabled = i == compartmentIndex;
            Arrays.stream(compartmentSlots[i]).forEach(slot -> slot.toggle(enabled));
        }

        if (tile.getLevel().isClientSide()) {
            TetraMod.packetHandler.sendToServer(new ChangeCompartmentPacket(compartmentIndex));
        }
    }

    private int getSlots() {
        var handler = tile.getResourceHandler(null);
        return handler != null ? handler.size() : 0;
    }

    /**
     * Take a stack from the specified inventory slot.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack resultStack = ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();

            resultStack = slotStack.copy();

            if (index < getSlots()) {
                if (!moveItemStackTo(slotStack, getSlots(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, currentCompartment * ForgedContainerBlockEntity.compartmentSize,
                    (currentCompartment + 1) * ForgedContainerBlockEntity.compartmentSize, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return resultStack;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()), playerIn, ForgedContainerBlock.instance.get());
    }

    public ForgedContainerBlockEntity getTile() {
        return tile;
    }
}
