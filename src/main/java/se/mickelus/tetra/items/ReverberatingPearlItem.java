package se.mickelus.tetra.items;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.Tooltips;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReverberatingPearlItem extends TetraItem {
    private static final String unlocalizedName = "reverberating_pearl";

    public ReverberatingPearlItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item." + unlocalizedName + ".tooltip"));
        tooltip.accept(Component.literal(" "));

        if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Tooltips.expanded);
            tooltip.accept(Tooltips.reveal);
            tooltip.accept(Component.literal(" "));
            tooltip.accept(Component.translatable("item." + unlocalizedName + ".tooltip_extended"));
        } else {
            tooltip.accept(Tooltips.expand);
        }
    }
}
