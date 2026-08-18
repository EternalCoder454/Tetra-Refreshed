package se.mickelus.tetra.items.modular;

import javax.annotation.Nullable;
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

        state.modular = itemStack.getItem() instanceof IModularItem modular ? modular : null;
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
        state.spin = entity.tickCount + partialTicks;
        state.dealtDamage = entity.hasDealtDamage();
        state.onGround = entity.onGround();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        // The renderer used to choose a pose here with a chain of instanceof against Tetra's own
        // item classes, which left an item from another mod with no way to get one short of mixing
        // into this method. The item is asked instead.
        if (state.modular != null) {
            state.modular.applyThrownPose(poseStack, state.yaw, state.pitch, state.spin, state.dealtDamage, state.onGround);
        }

        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        @Nullable
        public IModularItem modular;
        public float yaw;
        public float pitch;
        public float spin;
        public boolean dealtDamage;
        public boolean onGround;
    }
}
