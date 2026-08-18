package se.mickelus.tetra.module.data;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.mickelus.tetra.module.schematic.OutcomeMaterial;
import net.minecraft.core.component.DataComponentMap;

/**
 * Derives a material from any tool shaped item that no hand written material already covers.
 *
 * Compatibility with another mod's tools used to mean somebody writing a material by hand for every
 * one, which made it a permanent backlog rather than something that worked. This reads the stats
 * back off the item instead. A tool declares what repairs it, how fast it mines, what it can mine,
 * how much damage it takes and how enchantable it is, all as data components, so a material can be
 * built from the item itself without the mod knowing Tetra exists.
 *
 * The components are read rather than the ToolMaterial that usually writes them, because a
 * ToolMaterial is applied to an item's properties and not kept. Reading the result covers tools
 * assembled some other way too, which is most of the interesting cases.
 *
 * Anything authored wins. A generated material is only kept when no material in the pack already
 * claims its repair item, so writing one by hand is always the way to override what this guesses.
 */
public class MaterialGenerator {
    private static final Logger logger = LogManager.getLogger();

    /**
     * Below this the item is a toy rather than a tool, and generating a material for it produces
     * noise. Wooden tools sit at 59.
     */
    private static final int minimumDurability = 32;

    private MaterialGenerator() {
    }

    /**
     * Add a material for every eligible item that the given data does not already cover.
     */
    public static void contribute(Map<Identifier, MaterialData> materials) {
        Set<Item> claimed = claimedItems(materials);
        int generated = 0;

        for (Item item : BuiltInRegistries.ITEM) {
            MaterialData material = derive(item, claimed);
            if (material == null) {
                continue;
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            materials.put(Identifier.fromNamespaceAndPath("tetra",
                    "generated/" + id.getNamespace() + "/" + id.getPath()), material);
            generated++;
        }

        if (generated > 0) {
            logger.info("Generated {} material(s) from tools no material covers", generated);
        }
    }

    /**
     * Every item any material already accepts, so nothing authored is generated over.
     */
    private static Set<Item> claimedItems(Map<Identifier, MaterialData> materials) {
        Set<Item> claimed = new HashSet<>();
        for (MaterialData material : materials.values()) {
            if (material == null || material.material == null) {
                continue;
            }
            // Items rather than stacks. This runs inside a datapack reload, and building a stack
            // needs item components that are unbound until the reload finishes.
            claimed.addAll(material.material.getApplicableItems());
        }
        return claimed;
    }

    private static MaterialData derive(Item item, Set<Item> claimed) {
        // The item's own default components, not a stack's. A stack resolves them through the item's
        // registry holder, which is not bound while a datapack reload is running.
        DataComponentMap components = item.components();

        Repairable repairable = components.get(DataComponents.REPAIRABLE);
        Tool tool = components.get(DataComponents.TOOL);
        Integer durability = components.get(DataComponents.MAX_DAMAGE);

        // A tool that cannot be repaired gives no material to make it from, which is the one thing
        // this cannot guess.
        if (repairable == null || tool == null || durability == null || durability < minimumDurability) {
            return null;
        }

        List<Item> repairItems = new ArrayList<>();
        for (Holder<Item> holder : repairable.items()) {
            repairItems.add(holder.value());
        }
        if (repairItems.isEmpty() || repairItems.stream().anyMatch(claimed::contains)) {
            return null;
        }

        MaterialData material = new MaterialData();
        material.key = "generated/" + BuiltInRegistries.ITEM.getKey(item).getPath();
        material.category = "metal";
        material.durability = durability;
        material.toolEfficiency = tool.defaultMiningSpeed();
        material.toolLevel = miningLevel(tool);

        Enchantable enchantable = components.get(DataComponents.ENCHANTABLE);
        material.magicCapacity = enchantable != null ? enchantable.value() * 6 : 0;

        material.material = OutcomeMaterial.of(repairItems);

        return material;
    }

    /**
     * What the tool can mine, expressed the way Tetra orders tiers.
     *
     * A tool says which blocks it is correct for rather than naming a tier, so the tier is read back
     * from the vanilla tag it is allowed to break. Anything unrecognised sits at stone, which is the
     * safe end: a material that under promises costs a player nothing, one that over promises breaks
     * progression.
     */
    private static int miningLevel(Tool tool) {
        for (Tool.Rule rule : tool.rules()) {
            if (!rule.correctForDrops().orElse(false)) {
                continue;
            }
            for (Holder<Block> block : rule.blocks()) {
                if (block.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
                    return 3;
                }
                if (block.is(BlockTags.NEEDS_IRON_TOOL)) {
                    return 2;
                }
            }
        }
        return 1;
    }
}
