package se.mickelus.tetra.blocks.forged.hammer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HammerBaseRenderer implements BlockEntityRenderer<HammerBaseBlockEntity, HammerBaseRenderer.State> {
    public static final SpriteId material = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "block/forged_hammer/base_sheet"));
    public static ModelLayerLocation layer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, HammerBaseBlock.identifier), "main");

    private final ModelPart unpowered;
    private final ModelPart powered;

    private final ModelPart[] modulesA;
    private final ModelPart[] modulesB;


    private final ModelPart cellAunpowered;
    private final ModelPart cellBunpowered;
    private final ModelPart cellApowered;
    private final ModelPart cellBpowered;

    private final SpriteGetter sprites;

    public HammerBaseRenderer(BlockEntityRendererProvider.Context context) {
        sprites = context.sprites();
        ModelPart modelpart = context.bakeLayer(layer);

        unpowered = modelpart.getChild("unpowered");
        powered = modelpart.getChild("powered");
        HammerEffect[] effects = HammerEffect.values();
        modulesA = new ModelPart[effects.length];
        modulesB = new ModelPart[effects.length];
        for (int i = 0; i < effects.length; i++) {
            modulesA[i] = modelpart.getChild("moduleA" + i);
            modulesB[i] = modelpart.getChild("moduleB" + i);
        }
        cellAunpowered = modelpart.getChild("cellAunpowered");
        cellBunpowered = modelpart.getChild("cellBunpowered");
        cellApowered = modelpart.getChild("cellApowered");
        cellBpowered = modelpart.getChild("cellBpowered");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("unpowered", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(0, 0, 0, 16, 16, 16), PartPose.ZERO);

        parts.addOrReplaceChild("powered", CubeListBuilder.create()
                .texOffs(64, 0)
                .addBox(0, 0, 0, 16, 16, 16), PartPose.ZERO);

        HammerEffect[] effects = HammerEffect.values();
        for (int i = 0; i < effects.length; i++) {
            parts.addOrReplaceChild("moduleA" + i, CubeListBuilder.create()
                            .texOffs(i * 16, 32)
                            .addBox(0, 0, -16, 16, 16, 0, new CubeDeformation(0.03f)),
                    PartPose.offsetAndRotation(0, 0, 0, 0, -Mth.PI / 2f, 0));
            parts.addOrReplaceChild("moduleB" + i, CubeListBuilder.create()
                            .texOffs(i * 16, 32)
                            .addBox(-16, 0, 0, 16, 16, 0, new CubeDeformation(0.03f)),
                    PartPose.offsetAndRotation(0, 0, 0, 0, Mth.PI / 2f, 0));
        }


        parts.addOrReplaceChild("cellAunpowered", CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(5.5f, -19, 5.5f, 5, 3, 5),
                PartPose.offsetAndRotation(0, 0, 0, -Mth.PI / 2f, 0, 0));
        parts.addOrReplaceChild("cellApowered", CubeListBuilder.create()
                        .texOffs(48, 8)
                        .addBox(5.5f, -19, 5.5f, 5, 3, 5),
                PartPose.offsetAndRotation(0, 0, 0, -Mth.PI / 2f, 0, 0));


        parts.addOrReplaceChild("cellBunpowered", CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(5.5f, -3, -10.5f, 5, 3, 5),
                PartPose.offsetAndRotation(0, 0, 0, Mth.PI / 2f, 0, 0));
        parts.addOrReplaceChild("cellBpowered", CubeListBuilder.create()
                        .texOffs(48, 8)
                        .addBox(5.5f, -3, -10.5f, 5, 3, 5),
                PartPose.offsetAndRotation(0, 0, 0, Mth.PI / 2f, 0, 0));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HammerBaseBlockEntity tile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        state.visible = tile.hasLevel();
        state.facing = tile.getFacing().toYRot();
        state.functional = tile.isFunctional();

        state.hasCellA = tile.hasCellInSlot(0);
        state.cellAFuelled = state.hasCellA && tile.getCellFuel(0) > 0;
        state.hasCellB = tile.hasCellInSlot(1);
        state.cellBFuelled = state.hasCellB && tile.getCellFuel(1) > 0;

        HammerEffect effectA = tile.getEffect(true);
        HammerEffect effectB = tile.getEffect(false);
        state.moduleA = effectA != null ? effectA.ordinal() : -1;
        state.moduleB = effectB != null ? effectB.ordinal() : -1;
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

        submitPart(state.functional ? powered : unpowered, state, poseStack, collector);

        if (state.hasCellA) {
            submitPart(state.cellAFuelled ? cellApowered : cellAunpowered, state, poseStack, collector);
        }

        if (state.hasCellB) {
            submitPart(state.cellBFuelled ? cellBpowered : cellBunpowered, state, poseStack, collector);
        }

        if (state.moduleA >= 0) {
            submitPart(modulesA[state.moduleA], state, poseStack, collector);
        }

        if (state.moduleB >= 0) {
            submitPart(modulesB[state.moduleB], state, poseStack, collector);
        }

        poseStack.popPose();
    }

    private void submitPart(ModelPart part, State state, PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitModelPart(part, poseStack, material.renderType(id -> RenderTypes.entityCutout(id, false)),
                state.lightCoords, OverlayTexture.NO_OVERLAY, sprites.get(material), -1, state.breakProgress);
    }

    public static class State extends BlockEntityRenderState {
        public boolean visible;
        public float facing;
        public boolean functional;
        public boolean hasCellA;
        public boolean cellAFuelled;
        public boolean hasCellB;
        public boolean cellBFuelled;
        public int moduleA = -1;
        public int moduleB = -1;
    }
}
