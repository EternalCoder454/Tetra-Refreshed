package se.mickelus.tetra.data.provider;

import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.multischematic.MultiblockSchematicBlock;

import java.util.concurrent.CompletableFuture;

public class TetraTagsProvider extends TagsProvider<Block> {

    public TetraTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(packOutput, Registries.BLOCK, lookupProvider, modId);
    }

    /** tag() went with the provider's own appender, TagAppender wraps the raw builder now. */
    private TagAppender<ResourceKey<Block>, Block> appender(TagKey<Block> tag) {
        return TagAppender.forBuilder(getOrCreateRawBuilder(tag));
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        MultiblockSchematicEntry[] schematics = {
                new MultiblockSchematicEntry("stonecutter", 3, 2),
                new MultiblockSchematicEntry("earthpiercer", 2, 2),
                new MultiblockSchematicEntry("extractor", 3, 3)
        };

        var schematicsAppender = appender(BlockTags.create(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "multiblock_schematic")));
        for (MultiblockSchematicEntry schematic : schematics) {
            var tag = BlockTags.create(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, schematic.id));
            var appender = appender(tag);
            schematicsAppender.addTag(tag);
            for (int h = 0; h < schematic.width; h++) {
                for (int v = 0; v < schematic.height; v++) {
                    String id = String.format(MultiblockSchematicBlock.Builder.format, schematic.id, h, v);
                    appender.addOptional(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("tetra", id)));
                }
            }
        }
    }

    record MultiblockSchematicEntry(String id, int width, int height) {
    }
}
