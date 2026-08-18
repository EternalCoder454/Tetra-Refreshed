package se.mickelus.tetra.items.modular.impl.shield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.util.CastOptional;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Draws a modular shield as a set of model parts, one per module.
 *
 * BlockEntityWithoutLevelRenderer is gone and IClientItemExtensions no longer hands out a custom
 * renderer. An item that draws as something other than quads is a SpecialModelRenderer now,
 * registered by id and named in the item's model json, and it submits into the render pipeline
 * rather than drawing immediately.
 *
 * The renderer no longer registers itself as a reload listener either. It is rebaked from its
 * unbaked form whenever models reload, which is what the reload listener was there to force.
 */
@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class ModularShieldRenderer implements SpecialModelRenderer<ItemStack> {
    public static ModelLayerLocation layer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "item/shield"), "main");
    public static ModelLayerLocation bannerLayer = new ModelLayerLocation(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "item/shield_banner"),
            "main");

    private final SpriteGetter sprites;
    private final ModularShieldModel model;
    private final ModularShieldBannerModel bannerModel;

    public ModularShieldRenderer(SpriteGetter sprites, ModularShieldModel model, ModularShieldBannerModel bannerModel) {
        this.sprites = sprites;
        this.model = model;
        this.bannerModel = bannerModel;
    }

    @Nullable
    @Override
    public ItemStack extractArgument(ItemStack itemStack) {
        return itemStack.copy();
    }

    @Override
    public void submit(@Nullable ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
            int outlineColor) {
        if (itemStack == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        CastOptional.cast(itemStack.getItem(), ModularShieldItem.class)
                .stream()
                .map(item -> item.getModels(itemStack, null))
                .flatMap(Collection::stream)
                .filter(moduleModel -> moduleModel instanceof ShieldModuleModel)
                .map(moduleModel -> (ShieldModuleModel) moduleModel)
                .forEach(modelData -> {
                    ModelPart bannerPart = bannerModel.getModel(modelData.getModel().toString());
                    if (bannerPart != null) {
                        if (itemStack.has(DataComponents.BASE_COLOR)) {
                            submitBanner(itemStack, poseStack, collector, light, overlay, hasFoil);
                        }
                        return;
                    }

                    ModelPart modelPart = model.getModel(modelData.getModel().toString());
                    if (modelPart == null) {
                        return;
                    }

                    // Shield module textures are item/ paths, so they stitch onto the item atlas
                    // rather than the block one. A SpriteId names the atlas alongside the sprite,
                    // because Material lost its atlas half and is only the sprite now.
                    SpriteId spriteId = new SpriteId(TextureAtlas.LOCATION_ITEMS, modelData.getTexture());

                    float r = modelData.getTint().getRedFloat();
                    float g = modelData.getTint().getGreenFloat();
                    float b = modelData.getTint().getBlueFloat();
                    float a = modelData.getTint().getAlphaFloat();

                    // reset alpha to 1 if it's 0 to avoid mistakes & make things cleaner
                    a = a == 0 ? 1 : a;

                    collector.submitModelPart(modelPart, poseStack, spriteId.renderType(model.renderType()), light, overlay,
                            sprites.get(spriteId), hasFoil, false, ARGB.colorFromFloat(a, r, g, b), null, outlineColor);
                });

        poseStack.popPose();
    }

    private void submitBanner(ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil) {
        DyeColor baseColor = itemStack.get(DataComponents.BASE_COLOR);
        BannerPatternLayers patterns = itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        if (baseColor != null) {
            BannerRenderer.submitPatterns(sprites, poseStack, collector, light, overlay, bannerModel, Unit.INSTANCE, false, baseColor, patterns, null);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        model.root().getExtentsForGui(new PoseStack(), output);
    }

    /**
     * The unbaked half. Registered by id and named in the shield's model json, the same shape every
     * other model type registration takes now.
     */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final Identifier id = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "modular_shield");
        public static final MapCodec<Unbaked> mapCodec = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return new ModularShieldRenderer(context.sprites(),
                    new ModularShieldModel(context.entityModelSet().bakeLayer(layer)),
                    new ModularShieldBannerModel(context.entityModelSet().bakeLayer(bannerLayer)));
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<ItemStack>> type() {
            return mapCodec;
        }
    }
}
