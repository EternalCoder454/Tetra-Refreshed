package se.mickelus.tetra.blocks.forged;

import se.mickelus.tetra.blocks.BlockTooltip;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import se.mickelus.tetra.blocks.TetraBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.level.block.state.BlockBehaviour;

@ParametersAreNonnullByDefault
public class ForgedWallBlock extends TetraBlock implements BlockTooltip {
    public static final String identifier = "forged_wall";

    public ForgedWallBlock(BlockBehaviour.Properties properties) {
        super(ForgedBlockCommon.solid(properties));
    }

    public void appendBlockHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag advanced) {
        tooltip.accept(ForgedBlockCommon.locationTooltip);
    }
}
