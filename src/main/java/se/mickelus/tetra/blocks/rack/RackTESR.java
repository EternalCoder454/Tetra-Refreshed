package se.mickelus.tetra.blocks.rack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import se.mickelus.tetra.items.modular.impl.ModularBladedItem;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;
import se.mickelus.tetra.items.modular.impl.shield.ModularShieldItem;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import se.mickelus.mutil.util.ResourceHandlers;

@ParametersAreNonnullByDefault
public class RackTESR implements BlockEntityRenderer<RackTile, RackTESR.State> {

    private final ItemModelResolver itemModelResolver;

    public RackTESR(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RackTile tile, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);

        state.slots.clear();
        state.facing = tile.getBlockState().getValue(RackBlock.facingProp);

        var handler = tile.getResourceHandler(null);
        if (handler == null) {
            return;
        }

        int renderId = (int) tile.getBlockPos().asLong();
        for (int i = 0; i < handler.size(); i++) {
            ItemStack itemStack = ResourceHandlers.stackIn(handler, i);
            if (itemStack.isEmpty()) {
                continue;
            }

            ItemStackRenderState item = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(item, itemStack, ItemDisplayContext.FIXED, tile.getLevel(), null, renderId);
            state.slots.add(new Slot(i, item, poseFor(itemStack)));
        }
    }

    /** Which way an item hangs on the rack, decided from the item rather than from a baked model. */
    private static Pose poseFor(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModularShieldItem) {
            return Pose.shield;
        }
        if (itemStack.getItem() instanceof ModularBladedItem || itemStack.is(ItemTags.SWORDS)) {
            return Pose.blade;
        }
        if (itemStack.getItem() instanceof ModularCrossbowItemImpl || itemStack.getItem() instanceof CrossbowItem) {
            return Pose.crossbow;
        }
        return Pose.flat;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.slots.isEmpty()) {
            return;
        }

        Direction direction = state.facing;
        Direction itemDirection = direction.getCounterClockWise();

        poseStack.pushPose();
        poseStack.translate(0.5 - direction.getStepX() * 0.36, 0.7, 0.5 - direction.getStepZ() * 0.36);
        poseStack.scale(0.5f, 0.5f, 0.5f);

        for (Slot slot : state.slots) {
            poseStack.pushPose();
            poseStack.translate(itemDirection.getStepX() * (slot.index - 0.5), 0, itemDirection.getStepZ() * (slot.index - 0.5));
            poseStack.mulPose(direction.getRotation());

            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            switch (slot.pose) {
                case shield -> {
                    poseStack.translate(-0.25, 0, 0.16);
                    poseStack.scale(2, 2, 2);
                }
                case blade -> {
                    poseStack.translate(0, -0.2, 0);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
                }
                case crossbow -> {
                    poseStack.translate(0, -0.2, 0);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(225.0F));
                }
                case flat -> poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            }

            slot.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private enum Pose {
        shield, blade, crossbow, flat
    }

    private record Slot(int index, ItemStackRenderState item, Pose pose) {}

    public static class State extends BlockEntityRenderState {
        public final List<Slot> slots = new ArrayList<>();
        public Direction facing = Direction.NORTH;
    }
}
