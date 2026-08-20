package se.mickelus.tetra.blocks.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import se.mickelus.mutil.gui.ToggleableSlot;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class WorkbenchContainer extends AbstractContainerMenu {
    public static Supplier<MenuType<WorkbenchContainer>> containerType;

    private static final int slotDetailX = 48;
    private static final int slotDetailY = 102;
    private static final int schematicMaterialBaseX = 136;
    private static final int schematicMaterialBaseY = 5;
    private static final int schematicMaterialItemOffsetX = 10;
    private static final int schematicMaterialItemOffsetY = 1;

    private final WorkbenchTile workbench;

    private ToggleableSlot[] materialSlots = new ToggleableSlot[0];

    public WorkbenchContainer(int windowId, WorkbenchTile workbench, Container playerInventory, Player player) {
        super(containerType.get(), windowId);
        this.workbench = workbench;

        // material inventory
        var handler = workbench.getResourceHandler(null);
        if (handler != null) {
            addSlot(new ResourceHandlerSlot(handler, workbench.getIndexModifier(null), 0, 152, 58));

            materialSlots = new ToggleableSlot[3];
            for (int i = 0; i < materialSlots.length; i++) {
                materialSlots[i] = new ToggleableSlot(handler, workbench.getIndexModifier(null), i + 1,
                        getMaterialSlotX(i, materialSlots.length), getMaterialSlotY());
                addSlot(materialSlots[i]);
            }
        }


        // player inventory
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 3; y++) {
                addSlot(new Slot(playerInventory, y * 9 + x + 9, x * 17 + 84, y * 17 + 166));
            }
        }

        // player toolbar
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, i * 17 + 84, 221));
        }
    }

    private int getSlots() {
        var handler = workbench.getResourceHandler(null);
        return handler != null ? handler.size() : 0;
    }

    /**
     * Materials go back to the player when the screen closes.
     *
     * They used to sit in the workbench until something else emptied the slots, which happens when
     * the schematic or the slot changes, so a stack put in and then walked away from stayed there
     * until the next visit and looked lost. The tool in slot zero stays, which is the point of a
     * workbench holding one.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);

        // Server side only. Both sides call this, and the client copy would drop a second stack.
        if (!player.level().isClientSide() && workbench != null) {
            workbench.emptyMaterialSlots(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = workbench.getBlockPos();

        // based on Container.isWithinUsableDistance but with more generic blockcheck
        if (workbench.getLevel().getBlockState(workbench.getBlockPos()).getBlock() instanceof AbstractWorkbenchBlock) {
            return player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
        }

        return false;
    }

    /**
     * Take a stack from the specified inventory slot.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack resultStack = ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();

            resultStack = slotStack.copy();

            if (index < getSlots()) {
                if (!moveItemStackTo(slotStack, getSlots(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, 0, getSlots(), false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        workbench.setChanged();
        return resultStack;
    }

    public void updateSlots() {
        int numMaterialSlots = Optional.ofNullable(workbench.getCurrentSchematic())
                .map(UpgradeSchematic::getNumMaterialSlots)
                .orElse(0);

        for (int i = 0; i < materialSlots.length; i++) {
            // Slot#setPosition is gone; x and y are the public fields it wrote to.
            materialSlots[i].x = getMaterialSlotX(i, numMaterialSlots);
            materialSlots[i].y = getMaterialSlotY();
            materialSlots[i].toggle(i < numMaterialSlots);
        }
    }

    public static int getMaterialSlotGuiX(int index, int numMaterialSlots) {
        return schematicMaterialBaseX + getMaterialSlotOffsetX(index, numMaterialSlots);
    }

    public static int getMaterialSlotX(int index, int numMaterialSlots) {
        return slotDetailX + getMaterialSlotGuiX(index, numMaterialSlots) + schematicMaterialItemOffsetX;
    }

    public static int getMaterialSlotY() {
        return slotDetailY + schematicMaterialBaseY + schematicMaterialItemOffsetY;
    }

    public static int getMaterialSlotOffsetX(int index, int numMaterialSlots) {
        if (numMaterialSlots == 2) {
            return 11 + 32 * index;
        }

        return 28 * index;
    }

    public WorkbenchTile getTileEntity() {
        return workbench;
    }
}
