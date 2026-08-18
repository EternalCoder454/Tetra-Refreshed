package se.mickelus.tetra.blocks.geode;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import se.mickelus.tetra.Tooltips;
import se.mickelus.tetra.items.TetraItem;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * A gem recovered intact from a geode.
 *
 * There were five of these, one class per gem, and they were identical down to sharing the same
 * tooltip key. Which gem an item is, is its registry id and its texture, so it is one class
 * registered five times. Anything that has to differ per gem belongs in data rather than here.
 */
@ParametersAreNonnullByDefault
public class PristineGemItem extends TetraItem {
    public static final String lapisIdentifier = "pristine_lapis";
    public static final String emeraldIdentifier = "pristine_emerald";
    public static final String diamondIdentifier = "pristine_diamond";
    public static final String amethystIdentifier = "pristine_amethyst";
    public static final String quartzIdentifier = "pristine_quartz";

    public PristineGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag advanced) {
        if (Minecraft.getInstance().hasShiftDown()) {
            tooltip.accept(Tooltips.expanded);
            tooltip.accept(Component.translatable("item.tetra.pristine_gem.description").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Tooltips.expand);
        }
    }
}
