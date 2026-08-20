package se.mickelus.tetra.data.provider;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import se.mickelus.tetra.blocks.multischematic.MultiblockSchematicBlock;
import se.mickelus.tetra.util.RegistryHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static se.mickelus.tetra.TetraMod.MOD_ID;

/**
 * Generates the multiblock schematic blockstates and models.
 *
 * NeoForge's model generators went away with the rest of its model system, so this sits on
 * vanilla's ModelProvider now. The one thing worth naming is the rotation: the old call asked
 * for a horizontal block with a 90 degree offset, and ROTATION_TORCH is the dispatch with
 * exactly that mapping, east at zero through north at 270, despite what it is called.
 */
@ParametersAreNonnullByDefault
public class TetraBlockStateProvider extends ModelProvider {

    private static final ModelTemplate schematicTemplate = new ModelTemplate(
            Optional.of(Identifier.fromNamespaceAndPath(MOD_ID, "block/multi_schematic_base")),
            Optional.empty(),
            TextureSlot.PARTICLE, TextureSlot.SIDE, TextureSlot.FRONT, TextureSlot.BACK);

    public TetraBlockStateProvider(PackOutput packOutput) {
        super(packOutput, MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        setupMultiBlockSchematics(blockModels);
    }

    /** Only the blocks generated here, so the provider does not demand models for hand written blockstates. */
    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return schematicBlocks().stream().map(Block::builtInRegistryHolder);
    }

    /**
     * Only the items generated here, for the same reason as the blocks above.
     *
     * Narrowing the blocks alone is not enough. The item side validates separately and defaults to
     * every item in the namespace, so leaving it alone made the generator demand an item model
     * definition for all thirty of Tetra's hand written items and fail before writing anything.
     */
    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return schematicBlocks().stream().map(Block::asItem).map(Item::builtInRegistryHolder);
    }

    private static List<Block> schematicBlocks() {
        List<Block> blocks = new ArrayList<>();
        collect(blocks, "stonecutter", 3, 2, true);
        collect(blocks, "earthpiercer", 2, 2, true);
        collect(blocks, "extractor", 3, 3, true);
        return blocks;
    }

    private static void collect(List<Block> blocks, String identifier, int width, int height, boolean ruinable) {
        for (int h = 0; h < width; h++) {
            for (int v = 0; v < height; v++) {
                blocks.add(blockFor(identifier, h, v));
                if (ruinable) {
                    blocks.add(blockFor(identifier + "_ruined", h, v));
                }
            }
        }
    }

    private static Block blockFor(String identifier, int h, int v) {
        Identifier rl = Identifier.fromNamespaceAndPath(MOD_ID, String.format(MultiblockSchematicBlock.Builder.format, identifier, h, v));
        return Objects.requireNonNull(RegistryHelper.get(BuiltInRegistries.BLOCK, rl), "Unknown block: " + rl);
    }

    private void setupMultiBlockSchematics(BlockModelGenerators blockModels) {
        setupMultiBlockSchematics(blockModels, "stonecutter", 3, 2, true);
        setupMultiBlockSchematics(blockModels, "earthpiercer", 2, 2, true);
        setupMultiBlockSchematics(blockModels, "extractor", 3, 3, true);
    }

    private void setupMultiBlockSchematics(BlockModelGenerators blockModels, String identifier, int width, int height, boolean ruinable) {
        for (int h = 0; h < width; h++) {
            for (int v = 0; v < height; v++) {
                setupMultiBlockSchematic(blockModels, identifier, "block/forged_schematic/", h, v);
                if (ruinable) {
                    setupMultiBlockSchematic(blockModels, identifier + "_ruined", "block/forged_schematic/", h, v);
                }
            }
        }
    }

    private void setupMultiBlockSchematic(BlockModelGenerators blockModels, String identifier, String modelPrefix, int h, int v) {
        String id = String.format(MultiblockSchematicBlock.Builder.format, identifier, h, v);
        Block block = blockFor(identifier, h, v);

        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, material(modelPrefix + id))
                .put(TextureSlot.SIDE, material(modelPrefix + "side"))
                .put(TextureSlot.FRONT, material(modelPrefix + id))
                .put(TextureSlot.BACK, material(modelPrefix + "back"));

        Identifier model = schematicTemplate.create(block, textures, blockModels.modelOutput);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                        .with(BlockModelGenerators.ROTATION_TORCH));

        blockModels.registerSimpleItemModel(block, model);
    }

    private static Material material(String path) {
        return new Material(Identifier.fromNamespaceAndPath(MOD_ID, path));
    }
}
