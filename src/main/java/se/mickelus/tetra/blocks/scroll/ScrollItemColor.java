package se.mickelus.tetra.blocks.scroll;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * ItemColor is gone. Tinting is a tint source named in the item's model json and registered by id,
 * rather than a handler registered against an item, so this carries a codec. Which layer it applies
 * to is decided in the json now instead of by the tint index check that used to live here.
 */
@ParametersAreNonnullByDefault
public class ScrollItemColor implements ItemTintSource {
    public static final Identifier id = Identifier.fromNamespaceAndPath("tetra", "scroll_ribbon");
    public static final MapCodec<ScrollItemColor> mapCodec = MapCodec.unit(ScrollItemColor::new);

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return ScrollData.readRibbonFast(itemStack);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return mapCodec;
    }
}
