package se.mickelus.tetra.client.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import se.mickelus.tetra.blocks.scroll.ScrollData;

/**
 * ItemProperties.register is gone. A numeric model property is a registered type named in the item's
 * model json now, rather than a function attached to an item at client init.
 */
public record ScrollMaterialProperty() implements RangeSelectItemModelProperty {
    public static final Identifier id = Identifier.fromNamespaceAndPath("tetra", "scroll_mat");
    public static final MapCodec<ScrollMaterialProperty> mapCodec = MapCodec.unit(ScrollMaterialProperty::new);

    @Override
    public float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return ScrollData.readMaterialFast(itemStack);
    }

    @Override
    public MapCodec<ScrollMaterialProperty> type() {
        return mapCodec;
    }
}
