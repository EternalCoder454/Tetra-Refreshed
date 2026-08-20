package se.mickelus.tetra.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import se.mickelus.mutil.util.ResourceHandlers;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.forged.container.ForgedContainerBlock;
import se.mickelus.tetra.blocks.forged.container.ForgedContainerBlockEntity;
import se.mickelus.tetra.blocks.rack.RackBlock;
import se.mickelus.tetra.blocks.rack.RackTile;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;

import java.util.function.Consumer;

/**
 * That the three block inventories survive being saved and read back.
 *
 * The port moved these off IItemHandler, which is deprecated and marked for removal, onto
 * ResourceHandler. The three blocks that hold items now use an ItemStacksResourceHandler, which
 * writes under a different key than ItemStackHandler did, and nothing had ever verified that what
 * goes in comes back out. The failure mode is not a crash or a log line, it is a chest that is
 * empty when you open it again, which is the worst kind to find by playing.
 *
 * These run in a real server with a real world, because an ItemStack cannot be built in a unit test
 * here at all. NeoForge binds item components when a world loads, so there is no honest way to make
 * one before that, and a mock would be testing the mock.
 */
public class InventoryMigrationTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> testFunctions =
            DeferredRegister.create(Registries.TEST_FUNCTION, TetraMod.MOD_ID);

    static {
        testFunctions.register("workbench_keeps_its_inventory", () -> helper ->
                roundTrip(helper, BasicWorkbenchBlock.instance, WorkbenchTile.class,
                        tile -> tile.getResourceHandler(null)));

        testFunctions.register("rack_keeps_its_inventory", () -> helper ->
                roundTrip(helper, RackBlock.instance, RackTile.class,
                        tile -> tile.getResourceHandler(null)));

        testFunctions.register("forged_container_keeps_its_inventory", () -> helper ->
                roundTrip(helper, ForgedContainerBlock.instance.get(), ForgedContainerBlockEntity.class,
                        tile -> tile.getResourceHandler(null)));
    }

    /**
     * Fill the first slot, save the block entity, read it into a fresh one, and compare.
     *
     * Saving and reading rather than reloading the world, because that is the step the port
     * actually changed. A world reload would exercise the same two calls with a great deal more
     * machinery in the way, and would fail no differently.
     */
    private static <T extends BlockEntity> void roundTrip(GameTestHelper helper, Block block, Class<T> tileClass,
            java.util.function.Function<T, ResourceHandler<ItemResource>> handlerOf) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, block);

        T tile = helper.getBlockEntity(pos, tileClass);
        if (tile == null) {
            helper.fail("no block entity for " + block, pos);
            return;
        }

        ResourceHandler<ItemResource> handler = handlerOf.apply(tile);
        if (handler == null || handler.size() == 0) {
            helper.fail("no inventory on " + block, pos);
            return;
        }

        ItemStack stored = new ItemStack(Items.IRON_INGOT, 7);
        ItemStack rejected = ResourceHandlers.insert(handler, 0, stored.copy());
        if (!rejected.isEmpty()) {
            helper.fail("the inventory would not take a plain iron ingot", pos);
            return;
        }

        ItemStack beforeSave = ResourceHandlers.stackIn(handler, 0);
        if (beforeSave.getCount() != 7 || !beforeSave.is(Items.IRON_INGOT)) {
            helper.fail("what went in is not what the inventory holds, found " + beforeSave, pos);
            return;
        }

        // Save it the way the world does, then read it into a fresh block entity of the same kind.
        var registries = helper.getLevel().registryAccess();
        var saved = tile.saveWithoutMetadata(registries);

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(pos, block);

        T reloaded = helper.getBlockEntity(pos, tileClass);
        if (reloaded == null) {
            helper.fail("the block entity did not come back", pos);
            return;
        }
        reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

        ItemStack afterLoad = ResourceHandlers.stackIn(handlerOf.apply(reloaded), 0);
        if (afterLoad.isEmpty()) {
            helper.fail("the inventory was empty after saving and reading it back", pos);
            return;
        }
        if (!afterLoad.is(Items.IRON_INGOT) || afterLoad.getCount() != 7) {
            helper.fail("the contents changed across a save, expected 7 iron ingots and found " + afterLoad, pos);
            return;
        }

        helper.succeed();
    }
}
