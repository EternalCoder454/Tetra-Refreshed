package se.mickelus.tetra.blocks.multischematic;


import net.minecraft.util.ARGB;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import se.mickelus.mutil.util.RotationHelper;
import se.mickelus.tetra.ClientScheduler;
import se.mickelus.tetra.TetraItemAbilities;
import se.mickelus.tetra.blocks.salvage.BlockInteraction;
import se.mickelus.tetra.blocks.salvage.IInteractiveBlock;
import se.mickelus.tetra.effect.EffectHelper;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.function.UnaryOperator;

public class MultiblockSchematicBlock extends HorizontalDirectionalBlock implements IInteractiveBlock {
    public static final EnumProperty<Direction> facingProp = BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<MultiblockSchematicBlock> codec = MapCodec.unit(this);
    public final int x;
    public final int y;
    public final int height;
    public final int width;
    public final Supplier<RuinedMultiblockSchematicBlock> ruinedRef;
    protected String schematic;
    protected Identifier pryTable;
    protected BlockInteraction[] pryAction = new BlockInteraction[] {
            new BlockInteraction(TetraItemAbilities.pry, 1, Direction.EAST, 6, 10, 7, 10,
                    BlockStatePredicate.ANY,
                    this::pryBlock)
    };

    public MultiblockSchematicBlock(Properties properties, String schematic, Supplier<RuinedMultiblockSchematicBlock> ruinedRef,
            @Nullable Identifier pryTable, int x, int y, int height, int width) {
        super(properties);
        this.schematic = schematic;
        this.ruinedRef = ruinedRef;
        this.pryTable = pryTable;
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;

        this.registerDefaultState(this.stateDefinition.any().setValue(facingProp, Direction.EAST));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(facingProp);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(facingProp, context.getHorizontalDirection().getOpposite());
    }


    protected Stream<Part> getSchematicParts(BlockState blockState, LevelAccessor level, BlockPos blockPos) {
        Direction dir = blockState.getValue(facingProp);
        AABB baseBox = new AABB(0, 0, 0, width - 1, height - 1, 0);
        return BlockPos.betweenClosedStream(baseBox)
                .map(pos -> {
                    BlockPos worldPos = RotationHelper.rotateDirection(pos.offset(-x, -y, 0), dir).offset(blockPos);
                    return new Part(pos.immutable(), worldPos, level.getBlockState(worldPos));
                });
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldBlock, boolean p_60570_) {
        super.onPlace(blockState, level, blockPos, oldBlock, p_60570_);
        notifyPrimary(level, blockPos, blockState);

    }

    @Override
    public void destroy(LevelAccessor level, BlockPos blockPos, BlockState blockState) {
        super.destroy(level, blockPos, blockState);
        if (!level.isClientSide()) {
            notifyPrimary(level, blockPos, blockState);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity entity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, entity, itemStack);
        if (level.isClientSide()) {
            spawnParticles(blockState, level, blockPos);
        }
    }

    private InteractionResult useInternal(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (pryTable != null) {
            return BlockInteraction.attemptInteraction(world, state, pos, player, hand, hit);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (pryTable != null) {
            return useInternal(state, world, pos, player, hand, hit);
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (pryTable != null) {
            return useInternal(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public BlockInteraction[] getPotentialInteractions(Level world, BlockPos pos, BlockState blockState, Direction face,
            Collection<ItemAbility> tools) {
        if (pryTable != null && face.getOpposite().equals(blockState.getValue(facingProp))) {
            return pryAction;
        }
        return new BlockInteraction[0];
    }

    protected boolean pryBlock(Level world, BlockPos pos, BlockState blockState, Player player, InteractionHand hand, Direction facing) {
        boolean didBreak = EffectHelper.breakBlock(world, player, player.getItemInHand(hand), pos, blockState, false, false);
        if (didBreak && world instanceof ServerLevel) {
            BlockInteraction.getLoot(pryTable, player, hand, (ServerLevel) world, blockState)
                    .forEach(lootStack -> popResource(world, pos, lootStack));
        }

        return true;
    }

    protected void notifyPrimary(LevelAccessor level, BlockPos blockPos, BlockState blockState) {
        getSchematicParts(blockState, level, blockPos)
                .filter(part -> part.blockState.getBlock() instanceof PrimaryMultiblockSchematicBlock)
                .forEach(part ->
                        ((PrimaryMultiblockSchematicBlock) part.blockState.getBlock()).updateComplete(part.blockState(), level, part.worldPos(), blockPos));
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticles(BlockState blockState, Level level, BlockPos blockPos) {
        Vec3 face = Vec3.atLowerCornerOf(blockState.getValue(facingProp).getUnitVec3i());
        Vec3 dir = Vec3.atLowerCornerOf(blockState.getValue(facingProp).getClockWise().getUnitVec3i());
        getSchematicParts(blockState, level, blockPos).forEach(part ->
                ClientScheduler.schedule(blockPos.distManhattan(part.worldPos) * 2, () ->
                        spawnParticleBlock(level, blockState, part.basePos(), part.blockState(), part.worldPos(), face, dir)));
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticleBlock(Level level, BlockState originState, BlockPos basePos, BlockState blockState, BlockPos pos, Vec3 face,
            Vec3 dir) {
        Vec3 facePos = Vec3.atCenterOf(pos).add(face.scale(0.52));
        DustParticleOptions particle;
        if (blockState.getBlock() instanceof MultiblockSchematicBlock block && block.x == basePos.getX() && block.y == basePos.getY()) {
            particle = new DustParticleOptions(ARGB.color(26, 230, 128), 1f);
        } else {
            particle = new DustParticleOptions(ARGB.color(230, 76, 76), 1f);
        }

        spawnParticle(level, particle, facePos);
    }

    @OnlyIn(Dist.CLIENT)
    protected void spawnParticle(Level level, DustParticleOptions particle, Vec3 pos) {
        level.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    public record Part(BlockPos basePos, BlockPos worldPos, BlockState blockState) {
    }

    public static class Builder {
        public static final String format = "%s_%d_%d";
        public static final String ruinedFormat = "%s_ruined_%d_%d";
        public static final String pryTablePrefix = "actions/forged_schematic/";
        private final String identifier;
        private final int height;
        private final int width;

        // A Properties carries its block's registry id now, so the builder cannot hand the same
        // instance to every block it makes. It holds a decorator and applies it to each one.
        private final UnaryOperator<Properties> properties;
        private UnaryOperator<Properties> ruinedProperties;

        public Builder(String identifier, int width, int height, UnaryOperator<Properties> properties) {
            this.identifier = identifier;
            this.width = width;
            this.height = height;

            this.properties = properties;
            ruinedProperties = properties;
        }

        public Builder withRuinedProperties(UnaryOperator<Properties> properties) {
            this.ruinedProperties = properties;
            return this;
        }

        public void build(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
            if (FMLEnvironment.getDist().isClient()) {
                MultiblockSchematicScrollHandler.setupSchematic(identifier, width * height);
            }
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int x = i;
                    int y = j;

                    String ruinedId = String.format(ruinedFormat, identifier, x, y);
                    Identifier brokenPryTable = Identifier.fromNamespaceAndPath("tetra", pryTablePrefix + ruinedId);
                    Supplier<RuinedMultiblockSchematicBlock> ruinedRef =
                            blocks.registerBlock(ruinedId, props -> new RuinedMultiblockSchematicBlock(ruinedProperties.apply(props), brokenPryTable));

                    String id = String.format(format, identifier, x, y);
                    Identifier pryTable = Identifier.fromNamespaceAndPath("tetra", pryTablePrefix + id);
                    Supplier<MultiblockSchematicBlock> ref = x == width / 2 && y == height / 2
                            ? blocks.registerBlock(id, props -> new PrimaryMultiblockSchematicBlock(properties.apply(props), identifier, ruinedRef, pryTable, x, y, height, width))
                            : blocks.registerBlock(id, props -> new MultiblockSchematicBlock(properties.apply(props), identifier, ruinedRef, pryTable, x, y, height, width));


                    items.registerItem(id, props -> {
                        StackedMultiblockSchematicItem item = new StackedMultiblockSchematicItem(props, ref.get(), ruinedRef.get());
                        if (FMLEnvironment.getDist().isClient()) {
                            MultiblockSchematicScrollHandler.addSchematic(identifier, y * width + x, item);
                        }
                        return item;
                    });
                    items.registerItem(ruinedId, props -> new RuinedMultiblockSchematicItem(props, ruinedRef.get(), ref.get()));

                }
            }
        }
    }
}
