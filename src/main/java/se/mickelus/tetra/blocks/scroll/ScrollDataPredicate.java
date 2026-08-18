package se.mickelus.tetra.blocks.scroll;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.util.HexCodec;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
public record ScrollDataPredicate(
        Optional<String> key,
        Optional<String> details,
        Optional<Boolean> intricate,
        Optional<Integer> material,
        Optional<Integer> ribbon,
        Optional<List<Integer>> glyphs,
        Optional<List<Identifier>> schematics,
        Optional<List<Identifier>> effects
) implements DataComponentPredicate {
    public static final Codec<ScrollDataPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("key").forGetter(ScrollDataPredicate::key),
            Codec.STRING.optionalFieldOf("details").forGetter(ScrollDataPredicate::details),
            Codec.BOOL.optionalFieldOf("intricate").forGetter(ScrollDataPredicate::intricate),
            Codec.INT.optionalFieldOf("material").forGetter(ScrollDataPredicate::material),
            HexCodec.instance.optionalFieldOf("ribbon").forGetter(ScrollDataPredicate::ribbon),
            Codec.INT.listOf().optionalFieldOf("glyphs").forGetter(ScrollDataPredicate::glyphs),
            Identifier.CODEC.listOf().optionalFieldOf("schematics").forGetter(ScrollDataPredicate::schematics),
            Identifier.CODEC.listOf().optionalFieldOf("effects").forGetter(ScrollDataPredicate::effects)
    ).apply(instance, ScrollDataPredicate::new));
    public static final DataComponentPredicate.Type<ScrollDataPredicate> TYPE = new DataComponentPredicate.ConcreteType<>(CODEC);

    @Override
    public boolean matches(DataComponentGetter components) {
        return components instanceof ItemStack stack && matches(stack);
    }

    private boolean matches(ItemStack stack) {
        if (stack.getItem() != ScrollItem.instance) {
            return false;
        }

        return ScrollData.readOptional(stack)
                .map(this::matches)
                .orElse(false);
    }

    private boolean matches(ScrollData data) {
        return key.map(data.key::equals).orElse(true)
                && details.map(expected -> expected.equals(data.details)).orElse(true)
                && intricate.map(expected -> expected == data.isIntricate).orElse(true)
                && material.map(expected -> expected == data.material).orElse(true)
                && ribbon.map(expected -> expected == data.ribbon).orElse(true)
                && glyphs.map(expected -> expected.equals(data.glyphs)).orElse(true)
                && schematics.map(expected -> expected.equals(data.schematics)).orElse(true)
                && effects.map(expected -> expected.equals(data.craftingEffects)).orElse(true);
    }
}
