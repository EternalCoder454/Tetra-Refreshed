package se.mickelus.tetra.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/** The block item for a {@link BlockTooltip} block, which forwards the tooltip back to the block. */
@ParametersAreNonnullByDefault
public class TooltipBlockItem extends BlockItem {
    public TooltipBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag advanced) {
        super.appendHoverText(stack, context, display, tooltip, advanced);
        if (getBlock() instanceof BlockTooltip block) {
            block.appendBlockHoverText(stack, context, display, tooltip, advanced);
        }
    }
}
