package se.mickelus.tetra.blocks.forged.hammer;

import se.mickelus.tetra.blocks.BlockTooltip;
import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.util.TileEntityOptional;
import se.mickelus.tetra.TetraItemAbilities;
import se.mickelus.tetra.advancements.BlockUseCriterion;
import se.mickelus.tetra.blocks.TetraBlock;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;
import se.mickelus.tetra.blocks.salvage.BlockInteraction;
import se.mickelus.tetra.blocks.salvage.IInteractiveBlock;
import se.mickelus.tetra.blocks.salvage.InteractiveBlockOverlay;
import se.mickelus.tetra.blocks.salvage.TileBlockInteraction;
import se.mickelus.tetra.items.cell.ThermalCellItem;
import se.mickelus.tetra.module.ItemModuleMajor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Stream;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
import static net.minecraft.world.level.material.Fluids.WATER;
import static se.mickelus.tetra.blocks.forged.ForgedBlockCommon.locationTooltip;
import net.minecraft.world.level.block.state.BlockBehaviour;

@ParametersAreNonnullByDefault
public class HammerBaseBlock extends TetraBlock implements IInteractiveBlock, EntityBlock, BlockTooltip {
    public static final String identifier = "hammer_base";
    public static final EnumProperty<Direction> facingProp = HorizontalDirectionalBlock.FACING;

    public static final String qualityImprovementKey = "quality";
    public static final BlockInteraction[] interactions = new BlockInteraction[] {
            new TileBlockInteraction<>(TetraItemAbilities.pry, 1, Direction.EAST, 5, 11, 10, 12,
                    HammerBaseBlockEntity.class, tile -> tile.getEffect(true) != null,
                    (world, pos, blockState, player, hand, hitFace) -> removeModule(world, pos, blockState, player, hand, hitFace, true)),
            new TileBlockInteraction<>(TetraItemAbilities.pry, 1, Direction.WEST, 5, 11, 10, 12,
                    HammerBaseBlockEntity.class, tile -> tile.getEffect(false) != null,
                    (world, pos, blockState, player, hand, hitFace) -> removeModule(world, pos, blockState, player, hand, hitFace, false))
    };
    public static HammerBaseBlock instance;

    public HammerBaseBlock(BlockBehaviour.Properties properties) {
        super(ForgedBlockCommon.notSolid(properties));
        instance = this;
    }

    public static boolean removeModule(Level world, BlockPos pos, BlockState blockState, @Nullable Player player, @Nullable InteractionHand hand, Direction hitFace, boolean isA) {
        ItemStack moduleStack = TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .map(te -> te.removeModule(isA))
                .map(ItemStack::new)
                .orElse(null);

        if (moduleStack != null && !world.isClientSide()) {
            if (player != null && player.getInventory().add(moduleStack)) {
                player.playSound(SoundEvents.ITEM_PICKUP, 1, 1);
            } else {
                popResource(world, pos.relative(hitFace), moduleStack);
            }
        }

        world.playSound(player, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.5f, 0.6f);

        return true;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(facingProp);
    }

    public void appendBlockHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag advanced) {
        tooltip.accept(locationTooltip);
        tooltip.accept(Component.literal(" "));
        tooltip.accept(Component.translatable("block.multiblock_hint.1x2x1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public boolean isFunctional(Level world, BlockPos pos) {
        return TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .map(HammerBaseBlockEntity::isFunctional)
                .orElse(false);
    }

    public void consumeFuel(Level world, BlockPos pos) {
        TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .ifPresent(HammerBaseBlockEntity::consumeFuel);
    }

    public int getHammerLevel(Level world, BlockPos pos) {
        return TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .map(HammerBaseBlockEntity::getHammerLevel)
                .orElse(0);
    }

    public ItemStack applyCraftEffects(Level world, BlockPos pos, BlockState blockState, ItemStack targetStack, String slot, boolean isReplacing,
            Player player, ItemAbility requiredTool, int requiredLevel, boolean consumeResources) {
        if (consumeResources) {
            consumeFuel(world, pos);
        }

        if (isReplacing) {
            int preciseLevel = TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                    .map(te -> te.getEffectLevel(HammerEffect.precise))
                    .orElse(0);

            if (preciseLevel > 0) {
                ItemStack upgradedStack = targetStack.copy();

                ItemModuleMajor.addImprovement(upgradedStack, slot, qualityImprovementKey, preciseLevel);
                return upgradedStack;
            }
        }

        return targetStack;
    }

    public ItemStack applyActionEffects(Level world, BlockPos pos, BlockState blockState, ItemStack targetStack, Player player,
            ItemAbility requiredTool, int requiredLevel, boolean consumeResources) {
        if (consumeResources) {
            consumeFuel(world, pos);
        }
        return targetStack;
    }

    private Map<String, String> getAdvancementData(Level world, BlockPos pos) {
        return TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .map(tile -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("functional", String.valueOf(tile.isFunctional()));

                    Optional.ofNullable(tile.getEffect(true))
                            .ifPresent(module -> result.put("moduleA", module.toString()));
                    Optional.ofNullable(tile.getEffect(false))
                            .ifPresent(module -> result.put("moduleB", module.toString()));

                    return result;
                })
                .orElseGet(Collections::emptyMap);
    }

    private InteractionResult useInternal(final BlockState blockState, final Level world, final BlockPos pos, final Player player, final InteractionHand hand,
            final BlockHitResult rayTraceResult) {
        Direction blockFacing = blockState.getValue(facingProp);
        HammerBaseBlockEntity te = TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class).orElse(null);
        ItemStack heldStack = player.getItemInHand(hand);
        Direction facing = rayTraceResult.getDirection();

        if (te == null) {
            return InteractionResult.FAIL;
        }

        if (blockFacing.getAxis().equals(facing.getAxis())) {
            int slotIndex = blockFacing.equals(facing) ? 0 : 1;
            if (te.hasCellInSlot(slotIndex)) {
                if (!world.isClientSide()) {
                    ItemStack cell = te.removeCellFromSlot(slotIndex);
                    if (player.getInventory().add(cell)) {
                        player.playSound(SoundEvents.ITEM_PICKUP, 1, 1);
                    } else {
                        popResource(world, pos.relative(facing), cell);
                    }

                    BlockUseCriterion.trigger((ServerPlayer) player, world.getBlockState(pos), ItemStack.EMPTY, getAdvancementData(world, pos));
                    world.playSound(player, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.5f, 0.6f);
                }

                return InteractionResult.SUCCESS;
            } else if (heldStack.getItem() instanceof ThermalCellItem) {
                if (world.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                if (te.putCellInSlot(heldStack, slotIndex)) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                    BlockUseCriterion.trigger((ServerPlayer) player, world.getBlockState(pos), heldStack, getAdvancementData(world, pos));
                    world.playSound(player, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.5f, 0.5f);

                    return InteractionResult.CONSUME;
                }
            }
        } else {
            boolean isA = Rotation.CLOCKWISE_90.rotate(blockFacing).equals(facing);

            if (te.getEffect(isA) == null) {
                boolean success = te.setModule(isA, heldStack.getItem());
                if (success) {
                    if (!player.level().isClientSide()) {
                        BlockUseCriterion.trigger((ServerPlayer) player, world.getBlockState(pos), heldStack, getAdvancementData(world, pos));
                    }

                    world.playSound(player, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.5f, 0.5f);
                    heldStack.shrink(1);

                    if (world.isClientSide()) {
                        InteractiveBlockOverlay.markDirty();
                    }

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return BlockInteraction.attemptInteraction(world, world.getBlockState(pos), pos, player, hand, rayTraceResult);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState blockState, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult rayTraceResult) {
        return useInternal(blockState, world, pos, player, hand, rayTraceResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level world, BlockPos pos, Player player, BlockHitResult rayTraceResult) {
        return useInternal(blockState, world, pos, player, InteractionHand.MAIN_HAND, rayTraceResult);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean isMoving) {
        TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class)
                .ifPresent(tile -> {
                    for (int i = 0; i < 2; i++) {
                        if (tile.hasCellInSlot(i)) {
                            Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), tile.getStackInSlot(i).copy());
                        }
                    }

                    Stream.of(tile.getEffect(true), tile.getEffect(false))
                            .filter(Objects::nonNull)
                            .map(HammerEffect::getItem)
                            .map(ItemStack::new)
                            .forEach(stack -> Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack));
                });

        TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class).ifPresent(BlockEntity::setRemoved);
    
    }


    @Override
    public BlockInteraction[] getPotentialInteractions(Level world, BlockPos pos, final BlockState state, final Direction face, final Collection<ItemAbility> tools) {
        return Arrays.stream(interactions)
                .filter(interaction -> interaction.isPotentialInteraction(world, pos, state, state.getValue(facingProp), face, tools))
                .toArray(BlockInteraction[]::new);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        TileEntityOptional.from(level, pos, HammerBaseBlockEntity.class).ifPresent(HammerBaseBlockEntity::updateRedstonePower);
        if (Direction.DOWN.equals(direction) && !HammerHeadBlock.instance.equals(neighbourState.getBlock())) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // based on same method implementation in BedBlock
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockState headState = HammerHeadBlock.instance.defaultBlockState()
                .setValue(WATERLOGGED, world.getFluidState(pos.below()).getType() == WATER);
        world.setBlock(pos.below(), headState, 3);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos().below()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(facingProp, context.getHorizontalDirection().getOpposite());
        }

        // returning null here stops the block from being placed
        return null;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
        TileEntityOptional.from(world, pos, HammerBaseBlockEntity.class).ifPresent(HammerBaseBlockEntity::updateRedstonePower);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(facingProp, rotation.rotate(state.getValue(facingProp)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(facingProp)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HammerBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return getTicker(entityType, HammerBaseBlockEntity.type.get(), (lvl, pos, blockState, tile) -> tile.tick(lvl, pos, blockState));
    }
}
