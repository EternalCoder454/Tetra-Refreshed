package se.mickelus.tetra.blocks.forged.chthonic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class ExtractorProjectileRenderer extends EntityRenderer<ExtractorProjectileEntity, ExtractorProjectileRenderer.State> {

    public ExtractorProjectileRenderer(EntityRendererProvider.Context renderContext) {
        super(renderContext);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ExtractorProjectileEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // the projectile is drawn as the extractor block itself, which now goes through the same
        // moving block path a falling block uses rather than a resolved BakedModel
        BlockPos pos = entity.blockPosition();
        state.block.randomSeedPos = pos;
        state.block.blockPos = pos;
        state.block.blockState = ChthonicExtractorBlock.instance.defaultBlockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.block.biome = clientLevel.getBiome(pos);
            state.block.cardinalLighting = clientLevel.cardinalLighting();
            state.block.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 90.0F));
        poseStack.translate(-.3f, -.1f, -.45f);

        collector.submitMovingBlock(poseStack, state.block);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public final MovingBlockRenderState block = new MovingBlockRenderState();
        public float yaw;
        public float pitch;
    }
}
