package se.mickelus.tetra.blocks.workbench;

import se.mickelus.tetra.blocks.BlockTooltip;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.Tags;
import se.mickelus.tetra.advancements.BlockUseCriterion;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.level.block.state.BlockBehaviour;

@ParametersAreNonnullByDefault
public class BasicWorkbenchBlock extends AbstractWorkbenchBlock implements BlockTooltip {
    public static final String identifier = "basic_workbench";
    public static AbstractWorkbenchBlock instance;

    public BasicWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties
                .strength(2.5f)
                .sound(SoundType.WOOD));
    }

    public static InteractionResult upgradeWorkbench(Player player, Level world, BlockPos pos, InteractionHand hand, Direction facing) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!player.mayUseItemAt(pos.relative(facing), facing, itemStack)) {
            return InteractionResult.FAIL;
        }

        if (world.getBlockState(pos).is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {

            world.playSound(player, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.5F);

            if (!world.isClientSide()) {
                world.setBlockAndUpdate(pos, instance.defaultBlockState());

                BlockUseCriterion.trigger((ServerPlayer) player, instance.defaultBlockState(), ItemStack.EMPTY);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public void appendBlockHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag advanced) {
        tooltip.accept(Component.translatable("block.tetra.basic_workbench.description").withStyle(ChatFormatting.GRAY));
    }
}
