package se.mickelus.tetra.blocks.forged;

import se.mickelus.tetra.blocks.BlockTooltip;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SlabBlock;
import se.mickelus.tetra.blocks.InitializableBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class ForgedPlatformSlabBlock extends SlabBlock implements InitializableBlock, BlockTooltip {
    public static final String identifier = "forged_platform_slab";

    public ForgedPlatformSlabBlock() {
        super(ForgedBlockCommon.propertiesSolid);
    }

    public void appendBlockHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag advanced) {
        tooltip.accept(ForgedBlockCommon.locationTooltip);
    }
}
