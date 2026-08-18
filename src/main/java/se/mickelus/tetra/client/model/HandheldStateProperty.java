package se.mickelus.tetra.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import se.mickelus.tetra.items.modular.ItemModularHandheld;

/**
 * Whether a handheld item is being blocked or thrown with.
 *
 * These were registered as vanilla "blocking" and "throwing" item properties, which are gone along
 * with ItemProperties.register. A boolean model property is a registered conditional type named in
 * the item's model json now. One type covering both states keeps it to a single registration.
 */
public record HandheldStateProperty(State state) implements ConditionalItemModelProperty {
    public static final Identifier id = Identifier.fromNamespaceAndPath("tetra", "handheld_state");

    public static final MapCodec<HandheldStateProperty> mapCodec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            State.codec.fieldOf("state").forGetter(HandheldStateProperty::state)
    ).apply(instance, HandheldStateProperty::new));

    @Override
    public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed,
            ItemDisplayContext displayContext) {
        if (!(itemStack.getItem() instanceof ItemModularHandheld item)) {
            return false;
        }

        return switch (state) {
            case blocking -> item.isBlocking(itemStack, entity);
            case throwing -> item.isThrowing(itemStack, entity);
        };
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return mapCodec;
    }

    public enum State implements StringRepresentable {
        blocking,
        throwing;

        public static final Codec<State> codec = StringRepresentable.fromEnum(State::values);

        @Override
        public String getSerializedName() {
            return name();
        }
    }
}
