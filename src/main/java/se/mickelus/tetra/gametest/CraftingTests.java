package se.mickelus.tetra.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredRegister;
import se.mickelus.tetra.TetraItemAbilities;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.ModularBladedItem;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.data.ItemProperties;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * That a module can actually be crafted onto an item.
 *
 * This is the mod's whole purpose and nothing had ever run it. The port handover says as much:
 * the workbench opens and modular items render, but no crafting flow had been exercised. A green
 * build and a clean boot both say nothing about whether applying a schematic works, because every
 * part of it is data meeting reflection at runtime.
 *
 * The schematic is driven directly rather than through the workbench screen, because the screen is
 * a client thing and the part worth protecting is underneath it. What a player does through the gui
 * ends at applyUpgrade, and so does this.
 */
public class CraftingTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> testFunctions =
            DeferredRegister.create(Registries.TEST_FUNCTION, TetraMod.MOD_ID);

    private static final Identifier basicBlade = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "sword/basic_blade");
    private static final Identifier basicHilt = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "sword/basic_hilt/basic_hilt");
    private static final String bladeSlot = "sword/blade";
    private static final String hiltSlot = "sword/hilt";

    static {
        testFunctions.register("craft_a_module_onto_a_sword", () -> CraftingTests::craftsAModule);
        testFunctions.register("crafting_consumes_the_materials", () -> CraftingTests::consumesMaterials);
        testFunctions.register("crafting_refuses_without_materials", () -> CraftingTests::refusesEmptyHanded);
        testFunctions.register("crafting_refuses_an_integrity_violation", () -> CraftingTests::refusesIntegrityViolation);
        testFunctions.register("crafting_refuses_without_the_tool", () -> CraftingTests::refusesWithoutTheTool);
    }

    private static UpgradeSchematic schematic(GameTestHelper helper, Identifier key) {
        UpgradeSchematic found = SchematicRegistry.getSchematic(key);
        if (found == null) {
            helper.fail("the schematic " + key + " is not registered, so the data never loaded");
        }
        return found;
    }

    /**
     * A sword with a hilt on it, which is what a blade needs before it will fit.
     *
     * A hilt gives 3 integrity and a basic blade spends exactly that, so a bare sword cannot take
     * a blade at all. That is the rule the integrity test below pins, and the reason this helper
     * exists rather than every test starting from a bare item.
     */
    private static ItemStack hiltedSword(GameTestHelper helper, Player player) {
        UpgradeSchematic hilt = schematic(helper, basicHilt);
        if (hilt == null) {
            return ItemStack.EMPTY;
        }
        return hilt.applyUpgrade(bareSword(), new ItemStack[]{ new ItemStack(Items.IRON_INGOT, 1) },
                true, hiltSlot, player);
    }

    /**
     * A tool map offering a hammer at the given level, which is what a workbench beneath a
     * working forge hammer hands to a craft.
     */
    private static Map<ItemAbility, Integer> hammer(int level) {
        return Map.of(TetraItemAbilities.hammer, level);
    }

    private static ItemStack bareSword() {
        ItemStack sword = new ItemStack(ModularBladedItem.instance);
        IModularItem.updateIdentifier(sword);
        return sword;
    }

    private static void craftsAModule(GameTestHelper helper) {
        UpgradeSchematic schematic = schematic(helper, basicBlade);
        if (schematic == null) {
            return;
        }

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack sword = hiltedSword(helper, player);
        if (sword.isEmpty()) {
            return;
        }
        ItemStack[] materials = { new ItemStack(Items.IRON_INGOT, 2) };

        // The setup has to have worked before the thing under test means anything.
        IModularItem swordItem = (IModularItem) sword.getItem();
        if (swordItem.getModuleFromSlot(sword, hiltSlot) == null) {
            helper.fail("the hilt never went on, so this is not testing the blade at all");
            return;
        }

        if (!schematic.canApplyUpgrade(player, sword, materials, bladeSlot, hammer(2))) {
            // Name the reason. Refused covers four different rules and they fail identically.
            ItemProperties props = swordItem.getProperties(sword);
            helper.fail("a basic blade was refused."
                    + " materials ok: " + schematic.isMaterialsValid(sword, bladeSlot, materials)
                    + ", integrity now: " + props.integrity + "/" + props.integrityUsage
                    + ", tools wanted: " + schematic.getRequiredToolLevels(sword, materials));
            return;
        }

        ItemStack crafted = schematic.applyUpgrade(sword, materials, true, bladeSlot, player);

        IModularItem item = (IModularItem) crafted.getItem();
        ItemModule module = item.getModuleFromSlot(crafted, bladeSlot);
        if (module == null) {
            helper.fail("the craft reported success and the blade slot is still empty");
            return;
        }
        if (!module.getKey().equals("sword/basic_blade")) {
            helper.fail("expected a sword/basic_blade in the slot and found " + module.getKey());
            return;
        }
        // The hilt from the setup has to survive, or crafting replaces the item rather than
        // adding to it.
        if (item.getModuleFromSlot(crafted, hiltSlot) == null) {
            helper.fail("crafting the blade removed the hilt");
            return;
        }

        helper.succeed();
    }

    private static void consumesMaterials(GameTestHelper helper) {
        UpgradeSchematic schematic = schematic(helper, basicHilt);
        if (schematic == null) {
            return;
        }

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack sword = bareSword();
        // Deliberately more than the craft needs, so this measures what was taken rather than
        // whether the stack happened to empty.
        ItemStack[] materials = { new ItemStack(Items.IRON_INGOT, 8) };

        schematic.applyUpgrade(sword, materials, true, hiltSlot, player);

        int left = materials[0].getCount();
        if (left == 8) {
            helper.fail("the craft took no materials at all, so a blade is free");
            return;
        }
        if (left == 0) {
            helper.fail("the craft took the whole stack of 8 rather than what it needed");
            return;
        }

        helper.succeed();
    }

    private static void refusesEmptyHanded(GameTestHelper helper) {
        UpgradeSchematic schematic = schematic(helper, basicHilt);
        if (schematic == null) {
            return;
        }

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack sword = bareSword();
        ItemStack[] nothing = { ItemStack.EMPTY };

        if (schematic.canApplyUpgrade(player, sword, nothing, hiltSlot, Collections.emptyMap())) {
            helper.fail("a hilt could be crafted out of nothing");
            return;
        }

        helper.succeed();
    }

    /**
     * A blade wants a hammer at level 2, and without one the craft has to be refused.
     *
     * This is the rule behind the forge hammer being part of progression. The workbench gets its
     * hammer level from a working forge hammer above it, and if that ever silently returned
     * nothing then every blade in the game would still craft, which is exactly the kind of break
     * nobody would notice until the balance was gone.
     */
    private static void refusesWithoutTheTool(GameTestHelper helper) {
        UpgradeSchematic blade = schematic(helper, basicBlade);
        if (blade == null) {
            return;
        }

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack sword = hiltedSword(helper, player);
        if (sword.isEmpty()) {
            return;
        }
        ItemStack[] materials = { new ItemStack(Items.IRON_INGOT, 2) };

        if (blade.canApplyUpgrade(player, sword, materials, bladeSlot, Collections.emptyMap())) {
            helper.fail("a blade was forged with no hammer at all");
            return;
        }
        if (blade.canApplyUpgrade(player, sword, materials, bladeSlot, hammer(1))) {
            helper.fail("a blade wanting a hammer 2 was forged with a hammer 1");
            return;
        }
        if (!blade.canApplyUpgrade(player, sword, materials, bladeSlot, hammer(2))) {
            helper.fail("a hammer 2 is what the blade asks for and it was still refused");
            return;
        }

        helper.succeed();
    }

    /**
     * A blade needs integrity it can only get from a hilt, and asking anyway has to be refused.
     *
     * This is the rule that caught the first version of these tests out. A bare sword offers no
     * integrity, a basic blade spends three, and the workbench is supposed to say no. Worth
     * pinning, because a change that quietly stopped checking would make every module free.
     */
    private static void refusesIntegrityViolation(GameTestHelper helper) {
        UpgradeSchematic blade = schematic(helper, basicBlade);
        if (blade == null) {
            return;
        }

        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack[] materials = { new ItemStack(Items.IRON_INGOT, 2) };

        if (blade.canApplyUpgrade(player, bareSword(), materials, bladeSlot, Collections.emptyMap())) {
            helper.fail("a blade fitted onto a hiltless sword, so integrity is not being checked");
            return;
        }

        helper.succeed();
    }
}
