package se.mickelus.tetra.blocks.forged.hammer;

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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class HammerHeadRenderer implements BlockEntityRenderer<HammerHeadBlockEntity, HammerHeadRenderer.State> {
    private static final float animationDuration = 400;
    private static final float unjamDuration = 800;

    public HammerHeadRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HammerHeadBlockEntity tile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        double offset = Mth.clamp((1d * System.currentTimeMillis() - tile.getActivationTime()) / animationDuration, 0, 0.875);

        offset = Math.min(offset, Mth.clamp(0.25 + (1d * System.currentTimeMillis() - tile.getUnjamTime()) / unjamDuration, 0, 1) - 0.125);

        if (tile.isJammed()) {
            offset = Math.min(offset, 0.25f);
        }

        state.offset = offset;

        BlockPos pos = tile.getBlockPos();
        state.head.randomSeedPos = pos;
        state.head.blockPos = pos;
        state.head.blockState = HammerHeadBlock.instance.defaultBlockState();
        if (tile.getLevel() instanceof ClientLevel clientLevel) {
            state.head.biome = clientLevel.getBiome(pos);
            state.head.cardinalLighting = clientLevel.cardinalLighting();
            state.head.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, state.offset, 0);
        collector.submitMovingBlock(poseStack, state.head);
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final MovingBlockRenderState head = new MovingBlockRenderState();
        public double offset;
    }
}
