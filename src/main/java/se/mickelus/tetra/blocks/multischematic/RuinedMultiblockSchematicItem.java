package se.mickelus.tetra.blocks.multischematic;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class RuinedMultiblockSchematicItem extends BaseMultiblockSchematicItem {

    public RuinedMultiblockSchematicItem(Block ruinedBlock, MultiblockSchematicBlock block) {
        super(ruinedBlock, block);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(Component.translatable("block.tetra.multi_schematic.ruined")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.accept(Component.literal(" "));

        getTooltip().forEach(tooltip);
    }
}
