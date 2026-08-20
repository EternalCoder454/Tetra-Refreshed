package se.mickelus.tetra.blocks.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import se.mickelus.tetra.items.modular.impl.shield.ModularShieldItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WorkbenchTESR implements BlockEntityRenderer<WorkbenchTile, WorkbenchTESR.State> {

    private final ItemModelResolver itemModelResolver;

    public WorkbenchTESR(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WorkbenchTile workbenchTile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(workbenchTile, state, partialTicks, cameraPosition, breakProgress);

        state.item.clear();
        state.isShield = false;

        ItemStack itemStack = workbenchTile.getTargetItemStack();
        if (itemStack != null && !itemStack.isEmpty()) {
            int renderId = (int) workbenchTile.getBlockPos().asLong();
            itemModelResolver.updateForTopItem(state.item, itemStack, ItemDisplayContext.FIXED, workbenchTile.getLevel(), null, renderId);
            state.isShield = itemStack.getItem() instanceof ModularShieldItem;
        }

        // the item sits on top of the bench, so it is lit by the space above rather than the block
        if (workbenchTile.getLevel() != null) {
            state.lightCoords = LevelRenderer.getLightCoords(workbenchTile.getLevel(), workbenchTile.getBlockPos().above());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        if (state.isShield) {
            poseStack.translate(0.375, 0.9125, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else if (state.item.usesBlockLight()) {
            // stands block shaped items up, which is the question isGui3d used to answer
            poseStack.translate(0.5, 1.125, 0.5);
            poseStack.scale(.5f, .5f, .5f);
        } else {
            poseStack.translate(0.5, 1.0125, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.5f, 0.5f, 0.5f);
        }

        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean isShield;
    }
}
