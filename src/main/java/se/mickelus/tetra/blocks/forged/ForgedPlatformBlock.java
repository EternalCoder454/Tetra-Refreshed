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
import java.util.List;

@ParametersAreNonnullByDefault
public class ForgedPlatformBlock extends TetraBlock implements BlockTooltip {
    public static final String identifier = "forged_platform";

    public static ForgedPlatformBlock instance;

    public ForgedPlatformBlock() {
        super(ForgedBlockCommon.propertiesSolid);
    }

    public void appendBlockHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag advanced) {
        tooltip.accept(ForgedBlockCommon.locationTooltip);
    }
}
