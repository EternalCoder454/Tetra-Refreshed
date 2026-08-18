package se.mickelus.tetra.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A block that contributes lines to the tooltip of its own item.
 *
 * Block.appendHoverText does not exist in 26.1.2 and NeoForge does not add it back, so a block
 * has no way to speak for its item any more. Rather than move seventeen tooltips into item
 * registration and separate each one from the block it describes, the text stays on the block
 * and {@link TooltipBlockItem} asks for it.
 */
public interface BlockTooltip {
    void appendBlockHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag advanced);
}
