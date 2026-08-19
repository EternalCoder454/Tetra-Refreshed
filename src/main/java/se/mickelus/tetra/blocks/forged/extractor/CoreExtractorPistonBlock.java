package se.mickelus.tetra.blocks.forged.extractor;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.network.PacketHandler;
import se.mickelus.mutil.util.TileEntityOptional;
import se.mickelus.tetra.blocks.TetraWaterloggedBlock;
import se.mickelus.tetra.blocks.forged.ForgedBlockCommon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.RenderShape;

@ParametersAreNonnullByDefault
public class CoreExtractorPistonBlock extends TetraWaterloggedBlock implements EntityBlock {
    public static final String identifier = "extractor_piston";
    public static final BooleanProperty hackProp = BooleanProperty.create("hack");
    public static final VoxelShape boundingBox = box(5, 0, 5, 11, 16, 11);

    public static Supplier<CoreExtractorPistonBlock> instance;

    public CoreExtractorPistonBlock(BlockBehaviour.Properties properties) {
        super(ForgedBlockCommon.notSolid(properties));
    }

    @Override
    public void registerPackets(PacketHandler packetHandler) {
        packetHandler.registerPacket(CoreExtractorPistonUpdatePacket.class, CoreExtractorPistonUpdatePacket::new);
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        TileEntityOptional.from(worldIn, pos, CoreExtractorPistonBlockEntity.class)
                .ifPresent(te -> {
                    if (te.isActive()) {
                        float random = rand.nextFloat();

                        if (random < 0.6f) {
                            worldIn.addParticle(ParticleTypes.SMOKE,
                                    pos.getX() + 0.4 + rand.nextGaussian() * 0.2,
                                    pos.getY() + rand.nextGaussian(),
                                    pos.getZ() + 0.4 + rand.nextGaussian() * 0.2,
                                    0.0D, 0.0D, 0.0D);
                        }
                    }
                });
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (Direction.DOWN.equals(direction) && !CoreExtractorBaseBlock.instance.get().equals(neighbourState.getBlock())) {
            return state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return boundingBox;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(hackProp);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoreExtractorPistonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return getTicker(entityType, CoreExtractorPistonBlockEntity.type.get(), (lvl, pos, blockState, tile) -> tile.tick(lvl, pos, blockState));
    }

    /**
     * The block entity renderer draws this, so nothing may be baked into the chunk mesh.
     *
     * This returned ENTITYBLOCK_ANIMATED before the port. That constant is gone in 26.1.2, the enum
     * is INVISIBLE and MODEL now, and the override was dropped rather than mapped. Without it the
     * block falls back to MODEL, so the static cube and the renderer's cube both draw in the same
     * place and the faces flicker against each other.
     *
     * INVISIBLE rather than a model with no elements, which is how vanilla does chests, because the
     * item model here inherits from the block model. Emptying that would take the held item with it.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
