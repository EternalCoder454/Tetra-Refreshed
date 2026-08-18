package se.mickelus.tetra.blocks.forged.extractor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class CoreExtractorPistonRenderer implements BlockEntityRenderer<CoreExtractorPistonBlockEntity, CoreExtractorPistonRenderer.State> {

    public CoreExtractorPistonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoreExtractorPistonBlockEntity tile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        double offset = tile.getProgress(partialTicks);
        if (offset > 0.98) {
            // 49 = 0.98 / ( 1 - 0.98)
            offset = -49 * offset + 49;
        }
        state.offset = offset;

        BlockState base = CoreExtractorPistonBlock.instance.get().defaultBlockState();
        fill(state.shaft, tile, base.setValue(CoreExtractorPistonBlock.hackProp, true));
        fill(state.cover, tile, base.setValue(CoreExtractorPistonBlock.hackProp, false));
    }

    /** Both halves are the same block in two states, drawn through the moving block path. */
    private static void fill(MovingBlockRenderState block, CoreExtractorPistonBlockEntity tile, BlockState blockState) {
        BlockPos pos = tile.getBlockPos();
        block.randomSeedPos = pos;
        block.blockPos = pos;
        block.blockState = blockState;
        if (tile.getLevel() instanceof ClientLevel clientLevel) {
            block.biome = clientLevel.getBiome(pos);
            block.cardinalLighting = clientLevel.cardinalLighting();
            block.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        collector.submitMovingBlock(poseStack, state.shaft);

        poseStack.pushPose();
        poseStack.translate(0, state.offset, 0);
        collector.submitMovingBlock(poseStack, state.cover);
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final MovingBlockRenderState shaft = new MovingBlockRenderState();
        public final MovingBlockRenderState cover = new MovingBlockRenderState();
        public double offset;
    }
}
