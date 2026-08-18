package se.mickelus.tetra.items.modular;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.items.modular.impl.ModularBladedItem;
import se.mickelus.tetra.items.modular.impl.ModularDoubleHeadedItem;
import se.mickelus.tetra.items.modular.impl.ModularSingleHeadedItem;
import se.mickelus.tetra.items.modular.impl.shield.ModularShieldItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class ThrownModularItemRenderer extends EntityRenderer<ThrownModularItemEntity, ThrownModularItemRenderer.State> {

    private final ItemModelResolver itemModelResolver;

    public ThrownModularItemRenderer(EntityRendererProvider.Context manager) {
        super(manager);
        itemModelResolver = manager.getItemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownModularItemEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        ItemStack itemStack = entity.getPickupItem();
        state.item.clear();
        itemModelResolver.updateForNonLiving(state.item, itemStack, ItemDisplayContext.FIXED, entity);

        state.shape = shapeOf(itemStack.getItem());
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
        state.spin = entity.tickCount + partialTicks;
        state.dealtDamage = entity.hasDealtDamage();
        state.onGround = entity.onGround();
    }

    private static Shape shapeOf(Item item) {
        if (item instanceof ModularSingleHeadedItem) {
            return Shape.singleHeaded;
        }
        if (item instanceof ModularDoubleHeadedItem) {
            return Shape.doubleHeaded;
        }
        if (item instanceof ModularBladedItem) {
            return Shape.blade;
        }
        if (item instanceof ModularShieldItem) {
            return Shape.shield;
        }
        return Shape.none;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        switch (state.shape) {
            case singleHeaded -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 135.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(.3f, -.3f, 0);
            }
            case doubleHeaded -> {
                if (state.dealtDamage) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 135.0F));
                } else {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + state.spin));
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(.3f, -.3f, 0);
            }
            case blade -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 135.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }
            case shield -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
                if (state.onGround) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
                } else {
                    poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw + state.spin * 100));
                }
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(-0.2, 0, 0);
            }
            case none -> {
            }
        }

        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private enum Shape {
        singleHeaded, doubleHeaded, blade, shield, none
    }

    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public Shape shape = Shape.none;
        public float yaw;
        public float pitch;
        public float spin;
        public boolean dealtDamage;
        public boolean onGround;
    }
}
