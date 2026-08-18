package se.mickelus.tetra.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jetbrains.annotations.Nullable;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.model.GridTextureModelData;
import se.mickelus.tetra.util.ItemStackTagHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import org.joml.Vector3fc;
import java.util.function.Supplier;

/**
 * Renders a modular item as a stack of layers, one per module model.
 *
 * This replaces the old geometry loader outright. NeoForge's model geometry package is gone, and
 * with it BakedModel, ItemOverrides, IGeometryBakingContext and IQuadTransformer, so there is no
 * baked model left to swap per stack. Picking geometry per stack is what ItemModel#update does,
 * which is why ModularOverrideList collapsed into this class, cache and all.
 *
 * Each module model becomes one LayerRenderState. That is a better fit than the old single baked
 * model, because a layer already carries its own local transform, tint list and item transform, so
 * the per layer work that used to need quad transformers is state on the layer now. Only emissivity
 * still reaches into the quads, because it lives on a quad's material info.
 *
 * The per display context filtering that TetraSeparateTransformsModel did happens here too, since
 * update is handed the display context it is drawing for.
 */
public class ModularItemModel implements ItemModel {
    private static final Logger logger = LogManager.getLogger();
    private static final String layerSlot = "layer0";

    /**
     * Baking a layer means reading a sprite off the atlas and extruding it, far too slow to do per
     * frame. The cache is keyed the way the old override list keyed its baked models.
     */
    private final Cache<String, List<Layer>> layerCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final ItemTransforms transforms;
    private final Map<String, ItemTransforms> transformVariants;
    private final ModelBaker baker;
    private final Matrix4fc transformation;
    private final Identifier debugName;

    public ModularItemModel(ItemTransforms transforms, Map<String, ItemTransforms> transformVariants, ModelBaker baker,
            Matrix4fc transformation, Identifier debugName) {
        this.transforms = transforms;
        this.transformVariants = transformVariants;
        this.baker = baker;
        this.transformation = transformation;
        this.debugName = debugName;
    }

    public void clearCache() {
        logger.debug("Clearing item model cache for {}", debugName);
        layerCache.invalidateAll();
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack itemStack, ItemModelResolver resolver, ItemDisplayContext displayContext,
            @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (!(itemStack.getItem() instanceof IModularItem item)) {
            return;
        }

        CompoundTag baseTag = ItemStackTagHelper.readTag(itemStack);
        if (baseTag == null || baseTag.isEmpty()) {
            return;
        }

        LivingEntity entity = owner == null ? null : owner.asLivingEntity();
        String cacheKey = item.getModelCacheKey(itemStack, entity);

        List<Layer> layers;
        try {
            layers = layerCache.get(cacheKey, () -> bakeLayers(item, itemStack, entity));
        } catch (ExecutionException e) {
            logger.warn("Could not build item model layers for {}", debugName, e);
            return;
        }

        ItemTransform transform = getTransforms(item.getTransformVariant(itemStack, entity)).getTransform(displayContext);

        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(cacheKey);

        for (Layer layer : layers) {
            if (!layer.appliesTo(displayContext)) {
                continue;
            }

            ItemStackRenderState.LayerRenderState state = output.newLayer();
            // The gui fits an item to its extents. Without them a layer has nothing to be fitted to.
            state.setExtents(layer.extents());
            state.setItemTransform(transform);
            state.setLocalTransform(layer.transformation());
            if (layer.tint() != 0) {
                state.tintLayers().add(layer.tint());
            }
            state.prepareQuadList().addAll(layer.quads());
        }
    }

    /**
     * A variant only overrides the contexts it actually names, so anything it leaves out falls back
     * to the base transforms. ItemTransforms is a record now, so this reads through accessors.
     */
    private ItemTransforms getTransforms(@Nullable String transformVariant) {
        ItemTransforms variant = transformVariant != null ? transformVariants.get(transformVariant) : null;
        if (variant == null) {
            return transforms;
        }

        return new ItemTransforms(
                pick(variant.thirdPersonLeftHand(), transforms.thirdPersonLeftHand()),
                pick(variant.thirdPersonRightHand(), transforms.thirdPersonRightHand()),
                pick(variant.firstPersonLeftHand(), transforms.firstPersonLeftHand()),
                pick(variant.firstPersonRightHand(), transforms.firstPersonRightHand()),
                pick(variant.head(), transforms.head()),
                pick(variant.gui(), transforms.gui()),
                pick(variant.ground(), transforms.ground()),
                pick(variant.fixed(), transforms.fixed()),
                pick(variant.fixedFromBottom(), transforms.fixedFromBottom()));
    }

    private static ItemTransform pick(ItemTransform variant, ItemTransform fallback) {
        return ItemTransform.NO_TRANSFORM.equals(variant) ? fallback : variant;
    }

    private List<Layer> bakeLayers(IModularItem item, ItemStack itemStack, @Nullable LivingEntity entity) {
        List<Layer> layers = new ArrayList<>();

        for (var model : item.getModels(itemStack, entity)) {
            if (!(model instanceof GridTextureModelData textureModel)) {
                continue;
            }

            TextureSlots slots = new TextureSlots(Map.of(layerSlot, new Material(textureModel.getPaletteLocation())));
            QuadCollection collection = ItemModelGenerator.bake(slots, baker, BlockModelRotation.IDENTITY, debugName::toString);

            int emission = textureModel.getEmission();
            List<BakedQuad> quads = collection.getAll().stream()
                    .map(quad -> withEmission(quad, emission))
                    .toList();

            int tint = opaque(textureModel.getTint() != null ? textureModel.getTint().getRaw() : 0xffffffff);
            layers.add(new Layer(quads, tint == 0xffffffff ? 0 : tint, localTransform(textureModel.getTransform()),
                    textureModel.getContexts(), textureModel.isInvertPerspectives(),
                    Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads))));
        }

        return layers;
    }

    /**
     * Most module tints are written with no alpha channel at all, which reads as fully transparent.
     * Tetra has always taken an alpha of zero to mean opaque, which the quad transformer this model
     * replaced did for itself and the shield renderer still does. Without it every layer whose tint
     * omits the alpha byte draws nothing, which is why an obsidian head and the holosphere frame
     * were visible and every other module was not: those two are the ones written as ffffffff.
     */
    private static int opaque(int color) {
        return (color >>> 24) == 0 ? color | 0xff000000 : color;
    }

    /**
     * Emissivity is part of a quad's material info rather than something a quad transformer applies
     * afterwards, so a lit layer means rebuilding its quads around a new material info.
     */
    private static BakedQuad withEmission(BakedQuad quad, int emission) {
        if (emission <= 0 || emission >= 16 || quad.materialInfo().lightEmission() == emission) {
            return quad;
        }

        BakedQuad.MaterialInfo material = quad.materialInfo();
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
                new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(),
                        material.tintIndex(), material.shade(), emission, material.ambientOcclusion()));
    }

    private Matrix4fc localTransform(@Nullable Transformation transform) {
        if (transform == null || transform.isIdentity()) {
            return transformation;
        }
        return new Matrix4f(transformation).mul(transform.getMatrix());
    }

    private record Layer(List<BakedQuad> quads, int tint, Matrix4fc transformation, @Nullable ItemDisplayContext[] contexts,
            boolean invertContexts, Supplier<Vector3fc[]> extents) {
        boolean appliesTo(ItemDisplayContext displayContext) {
            return contexts == null || ArrayUtils.contains(contexts, displayContext) != invertContexts;
        }
    }

    /**
     * The unbaked half. A model type is registered into the item model type registry by id and named
     * in the item's model json, which is what replaced the geometry loader.
     */
    public record Unbaked(ItemTransforms transforms, Map<String, ItemTransforms> variants) implements ItemModel.Unbaked {
        public static final Identifier id = Identifier.fromNamespaceAndPath("tetra", "modular");

        public static final MapCodec<Unbaked> mapCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TransformCodecs.transforms.optionalFieldOf("display", ItemTransforms.NO_TRANSFORMS).forGetter(Unbaked::transforms),
                Codec.unboundedMap(Codec.STRING, TransformCodecs.transforms).optionalFieldOf("variants", Map.of()).forGetter(Unbaked::variants)
        ).apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModularItemModel model = new ModularItemModel(transforms, variants, context.blockModelBaker(), transformation, id);
            ModularModelLoader.addModel(model);
            return model;
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return mapCodec;
        }
    }
}
