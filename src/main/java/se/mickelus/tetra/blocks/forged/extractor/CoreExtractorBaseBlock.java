package se.mickelus.tetra.blocks.forged.extractor;

import se.mickelus.tetra.blocks.BlockTooltip;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.util.TileEntityOptional;
import se.mickelus.tetra.blocks.TetraWaterloggedBlock;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
import static net.minecraft.world.level.material.Fluids.WATER;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.BlockBehaviour;

@ParametersAreNonnullByDefault
public class CoreExtractorBaseBlock extends TetraWaterloggedBlock implements EntityBlock, BlockTooltip {
    public static final String identifier = "core_extractor";
    public static final EnumProperty<Direction> facingProp = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape capShape = box(3, 14, 3, 13, 16, 13);
    private static final VoxelShape shaftShape = box(4, 13, 4, 12, 14, 12);
    private static final VoxelShape smallCoverShapeZ = box(1, 0, 0, 15, 12, 16);
    private static final VoxelShape largeCoverShapeZ = box(0, 0, 1, 16, 13, 15);
    private static final VoxelShape smallCoverShapeX = box(0, 0, 1, 16, 12, 15);
    private static final VoxelShape largeCoverShapeX = box(1, 0, 0, 15, 13, 16);
    private static final VoxelShape combinedShapeZ
            = Shapes.or(Shapes.joinUnoptimized(smallCoverShapeZ, largeCoverShapeZ, BooleanOp.OR), capShape, shaftShape);
    private static final VoxelShape combinedShapeX
            = Shapes.or(Shapes.joinUnoptimized(smallCoverShapeX, largeCoverShapeX, BooleanOp.OR), capShape, shaftShape);
    public static Supplier<CoreExtractorBaseBlock> instance;

    public CoreExtractorBaseBlock(BlockBehaviour.Properties properties) {
        super(ForgedBlockCommon.notSolid(properties));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (Direction.Axis.X.equals(state.getValue(facingProp).getAxis())) {
            return combinedShapeX;
        }

        return combinedShapeZ;
    }

    public void appendBlockHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(ForgedBlockCommon.locationTooltip);
        tooltip.accept(Component.literal(" "));
        tooltip.accept(Component.translatable("block.multiblock_hint.1x2x1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /**
     * neighborChanged no longer carries the position the change came from. Orientation replaces it
     * and describes a propagation direction rather than a source block, and vanilla passes null for
     * it on ordinary neighbour updates, so the old "ignore the block we output into" guard cannot be
     * reconstructed. Recomputing unconditionally reaches the same state, only slightly more often.
     * setSending and setReceiving write with UPDATE_CLIENTS alone, so this cannot feed back.
     */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block fromBlock, @Nullable Orientation orientation, boolean isMoving) {
        TileEntityOptional.from(world, pos, CoreExtractorBaseBlockEntity.class)
                .ifPresent(CoreExtractorBaseBlockEntity::updateTransferState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(facingProp);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (Direction.UP.equals(direction) && !neighbourState.is(CoreExtractorPistonBlock.instance.get())) {
            return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // based on same method implementation in BedBlock
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockState pistonState = CoreExtractorPistonBlock.instance.get().defaultBlockState()
                .setValue(WATERLOGGED, world.getFluidState(pos.above()).getType() == WATER);
        world.setBlock(pos.above(), pistonState, 3);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context)) {
            return super.getStateForPlacement(context)
                    .setValue(facingProp, context.getHorizontalDirection().getOpposite());
        }

        // returning null here stops the block from being placed
        return null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction) {
        return state.setValue(facingProp, direction.rotate(state.getValue(facingProp)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(facingProp)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreExtractorBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return getTicker(entityType, CoreExtractorBaseBlockEntity.type, (lvl, pos, blockState, tile) -> tile.tick(lvl, pos, blockState));
    }
}
