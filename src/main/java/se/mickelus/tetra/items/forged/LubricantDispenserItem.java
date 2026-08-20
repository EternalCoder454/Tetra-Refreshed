package se.mickelus.tetra.items.forged;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.function.Supplier;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;
import se.mickelus.tetra.items.TetraItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LubricantDispenserItem extends TetraItem {
    public static final String identifier = "lubricant_dispenser";
    public static Supplier<LubricantDispenserItem> instance;

    public LubricantDispenserItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.tetra.lubricant_dispenser.description").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal(" "));
        tooltip.accept(ForgedBlockCommon.locationTooltip);
    }
}
