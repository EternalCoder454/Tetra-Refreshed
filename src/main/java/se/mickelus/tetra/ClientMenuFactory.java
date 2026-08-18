package se.mickelus.tetra;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.blocks.forged.container.ForgedContainerBlockEntity;
import se.mickelus.tetra.blocks.forged.container.ForgedContainerMenu;
import se.mickelus.tetra.blocks.workbench.WorkbenchContainer;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;

/**
 * Builds the menus that need the client's own level and player.
 *
 * These were static factories on the menus themselves, behind an @OnlyIn(Dist.CLIENT). NeoForge
 * does not strip @OnlyIn from mod classes, so Minecraft.getInstance().player left a LocalPlayer
 * reference in the menu's own bytecode, and a menu has to load on a dedicated server. Verifying the
 * class there threw NoClassDefFoundError before any menu could register. Keeping the client lookups
 * in a client only class is what @OnlyIn was being asked to do and cannot.
 */
@OnlyIn(Dist.CLIENT)
public class ClientMenuFactory {
    private ClientMenuFactory() {
    }

    public static WorkbenchContainer workbench(int windowId, BlockPos pos, Inventory inv) {
        WorkbenchTile tile = (WorkbenchTile) Minecraft.getInstance().level.getBlockEntity(pos);
        return new WorkbenchContainer(windowId, tile, inv, Minecraft.getInstance().player);
    }

    public static ForgedContainerMenu forgedContainer(int windowId, BlockPos pos, Inventory inv) {
        ForgedContainerBlockEntity tile = (ForgedContainerBlockEntity) Minecraft.getInstance().level.getBlockEntity(pos);
        return new ForgedContainerMenu(windowId, tile, inv, Minecraft.getInstance().player);
    }
}
