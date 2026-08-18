package se.mickelus.tetra.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Codecs for the display transform block Tetra's modular item models carry.
 *
 * Vanilla has none of its own. Display transforms are part of a block model now and reach an item
 * model through ModelRenderProperties, read by the model loader rather than by a codec. Tetra needs
 * them named per transform variant, which a block model has no room for, so they are read here
 * instead. The json shape is the one Tetra's model files already use, so those files do not move.
 */
public final class TransformCodecs {
    private static final Vector3fc noTranslation = new Vector3f();
    private static final Vector3fc noScale = new Vector3f(1, 1, 1);

    private TransformCodecs() {
    }

    public static final Codec<ItemTransform> transform = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", noTranslation).forGetter(ItemTransform::rotation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("translation", noTranslation).forGetter(ItemTransform::translation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("scale", noScale).forGetter(ItemTransform::scale),
            ExtraCodecs.VECTOR3F.optionalFieldOf("right_rotation", noTranslation).forGetter(ItemTransform::rightRotation)
    ).apply(instance, ItemTransform::new));

    public static final Codec<ItemTransforms> transforms = Codec.unboundedMap(ItemDisplayContext.CODEC, transform)
            .xmap(TransformCodecs::fromMap, TransformCodecs::toMap);

    private static ItemTransforms fromMap(Map<ItemDisplayContext, ItemTransform> map) {
        return new ItemTransforms(
                map.getOrDefault(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                map.getOrDefault(ItemDisplayContext.ON_SHELF, ItemTransform.NO_TRANSFORM));
    }

    private static Map<ItemDisplayContext, ItemTransform> toMap(ItemTransforms transforms) {
        return Map.of(
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, transforms.thirdPersonLeftHand(),
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transforms.thirdPersonRightHand(),
                ItemDisplayContext.FIRST_PERSON_LEFT_HAND, transforms.firstPersonLeftHand(),
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transforms.firstPersonRightHand(),
                ItemDisplayContext.HEAD, transforms.head(),
                ItemDisplayContext.GUI, transforms.gui(),
                ItemDisplayContext.GROUND, transforms.ground(),
                ItemDisplayContext.FIXED, transforms.fixed(),
                ItemDisplayContext.ON_SHELF, transforms.fixedFromBottom());
    }
}
