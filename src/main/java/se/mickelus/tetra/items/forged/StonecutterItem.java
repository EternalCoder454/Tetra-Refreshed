package se.mickelus.tetra.items.forged;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.Tooltips;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;
import se.mickelus.tetra.items.TetraItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class StonecutterItem extends TetraItem {
    public static final String identifier = "stonecutter";
    public static StonecutterItem instance;

    public StonecutterItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(ForgedBlockCommon.unsettlingTooltip);
        tooltip.accept(Component.literal(" "));

        if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Tooltips.expanded);
            tooltip.accept(Component.translatable("item.tetra.stonecutter.description").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.literal(" "));
            tooltip.accept(ForgedBlockCommon.locationTooltip);
        } else {
            tooltip.accept(Tooltips.expand);
        }
    }
}
