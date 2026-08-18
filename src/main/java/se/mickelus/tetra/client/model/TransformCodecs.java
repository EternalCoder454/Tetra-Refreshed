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

    /**
     * A display transform's translation is written in sixteenths of a block, the same units the rest
     * of a model uses, and vanilla scales it by a sixteenth when it reads one. Passing the raw value
     * through put a held item metres from the hand rather than centimetres. The clamps are vanilla's
     * too, and are what stop a bad value throwing the model out of the world entirely.
     */
    private static final float translationScale = 0.0625f;
    private static final float maxTranslation = 5.0f;
    private static final float maxScale = 4.0f;

    public static final Codec<ItemTransform> transform = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", noTranslation).forGetter(ItemTransform::rotation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("translation", noTranslation).forGetter(TransformCodecs::rawTranslation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("scale", noScale).forGetter(ItemTransform::scale),
            ExtraCodecs.VECTOR3F.optionalFieldOf("right_rotation", noTranslation).forGetter(ItemTransform::rightRotation)
    ).apply(instance, (rotation, translation, scale, rightRotation) -> new ItemTransform(
            rotation, scaledTranslation(translation), clampedScale(scale), rightRotation)));

    private static Vector3fc scaledTranslation(Vector3fc translation) {
        return new Vector3f(
                clamp(translation.x() * translationScale, maxTranslation),
                clamp(translation.y() * translationScale, maxTranslation),
                clamp(translation.z() * translationScale, maxTranslation));
    }

    private static Vector3fc clampedScale(Vector3fc scale) {
        return new Vector3f(clamp(scale.x(), maxScale), clamp(scale.y(), maxScale), clamp(scale.z(), maxScale));
    }

    /** Undoes the scaling, so that writing a transform back out gives the numbers it was read from. */
    private static Vector3fc rawTranslation(ItemTransform transform) {
        Vector3fc translation = transform.translation();
        return new Vector3f(translation.x(), translation.y(), translation.z()).mul(1 / translationScale);
    }

    private static float clamp(float value, float limit) {
        return Math.max(-limit, Math.min(limit, value));
    }

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
