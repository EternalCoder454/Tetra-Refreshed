package se.mickelus.tetra.blocks.forged.container;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class ForgedContainerRenderer implements BlockEntityRenderer<ForgedContainerBlockEntity, ForgedContainerRenderer.State> {
    public static final SpriteId material = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "block/forged_container/forged_container"));
    private static final float openDuration = 300;
    public static ModelLayerLocation layer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, ForgedContainerBlock.identifier), "main");
    public ModelPart lid;
    public ModelPart base;
    public ModelPart[] locks;
    private final SpriteGetter sprites;

    public ForgedContainerRenderer(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
        ModelPart modelpart = context.bakeLayer(layer);
        this.lid = modelpart.getChild("lid");

        locks = new ModelPart[4];
        for (int i = 0; i < 4; i++) {
            this.locks[i] = modelpart.getChild("locks" + i);
        }
        this.base = modelpart.getChild("base");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("lid", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(1, 4, 1, 30, 3, 14), PartPose.ZERO);
//        lid.x = 1;
//        lid.y = 7;
//        lid.z = 15;

        for (int i = 0; i < 4; i++) {
            parts.addOrReplaceChild("locks" + i, CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(6 + i * 6, 6, 0.97f, 2, 3, 1), PartPose.ZERO);
//            locks[i].x = 8;
//            locks[i].y = 7;
//            locks[i].z = 15;
        }

        parts.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(0, 17)
                .addBox(1, 7, 1, 30, 9, 14), PartPose.ZERO);
//        base.x = 1;
//        base.y = 6;
//        base.z = 1;

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ForgedContainerBlockEntity tile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        state.visible = !tile.isFlipped() && tile.hasLevel();
        state.facing = tile.getFacing().toYRot();
        state.open = tile.isOpen();
        state.openProgress = state.open ? Math.min(1, (System.currentTimeMillis() - tile.openTime) / openDuration) : 0;

        Boolean[] locked = tile.isLocked();
        for (int i = 0; i < state.locked.length; i++) {
            state.locked[i] = locked[i];
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // todo: why does the model render upside down by default?
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.facing));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        lid.yRot = state.openProgress * 0.1f * ((float) Math.PI / 2F);
        poseStack.translate(0, 0, 0.3f * state.openProgress);
        submitPart(lid, state, poseStack, collector);
        poseStack.translate(0, 0, -0.3f * state.openProgress);

        for (int i = 0; i < locks.length; i++) {
            if (state.locked[i]) {
                submitPart(locks[i], state, poseStack, collector);
            }
        }

        submitPart(base, state, poseStack, collector);

        poseStack.popPose();
    }

    private void submitPart(ModelPart part, State state, PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitModelPart(part, poseStack, material.renderType(RenderTypes::entitySolid), state.lightCoords,
                OverlayTexture.NO_OVERLAY, sprites.get(material), -1, state.breakProgress);
    }

    public static class State extends BlockEntityRenderState {
        public final boolean[] locked = new boolean[4];
        public boolean visible;
        public boolean open;
        public float facing;
        public float openProgress;
    }
}
