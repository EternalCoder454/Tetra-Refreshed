package se.mickelus.tetra.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import java.util.function.Supplier;
import se.mickelus.tetra.TetraRegistries;
import se.mickelus.tetra.blocks.scroll.ScrollData;
import se.mickelus.tetra.blocks.scroll.ScrollItem;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

@ParametersAreNonnullByDefault
public class ScrollIngredient implements ICustomIngredient {
    public static Supplier<IngredientType<ScrollIngredient>> type;
    public static final MapCodec<ScrollIngredient> CODEC = ScrollData.MAP_CODEC.xmap(ScrollIngredient::new, ingredient -> ingredient.data);
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollIngredient> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    private final ScrollData data;

    public ScrollIngredient(ScrollData data) {
        this.data = data;
    }

    @Override
    public boolean test(ItemStack input) {
        return !input.isEmpty()
                && input.getItem() == ScrollItem.instance
                && data.key.equals(ScrollData.read(input).key);
    }

    /**
     * ICustomIngredient.items lists item holders rather than stacks, so the scroll data this
     * ingredient matches on cannot be carried here. test still checks it, and display carries it.
     */
    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(ScrollItem.instance.builtInRegistryHolder());
    }

    /**
     * What a recipe shows for this ingredient. Without this it is whatever items() lists, which is a
     * scroll carrying no data at all, so every scroll ingredient read as the generic "Scroll" rather
     * than naming the schematic it holds.
     *
     * A template rather than a stack on purpose. Displays are built while recipes load, and item
     * components are unbound for the whole of a reload, so constructing a stack here would throw.
     */
    @Override
    public SlotDisplay display() {
        return new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(
                ScrollItem.instance.builtInRegistryHolder(),
                1,
                DataComponentPatch.builder().set(TetraRegistries.scrollData.get(), data).build()));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return type.get();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ScrollIngredient ingredient)) {
            return false;
        }

        return Objects.equals(data, ingredient.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
