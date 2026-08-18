package se.mickelus.tetra.blocks;

import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ScheduledTickAccess;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;
import se.mickelus.tetra.ServerScheduler;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.TetraSounds;
import se.mickelus.tetra.client.particle.Particles;
import se.mickelus.tetra.client.particle.SpawnParticlesPacket;
import se.mickelus.tetra.effect.potion.UnstablePowerMobEffect;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.entity.InsideBlockEffectApplier;

@ParametersAreNonnullByDefault
public class ArcaneFireBlock extends BaseFireBlock {
    public static final String identifier = "arcane_fire";
    public static final IntegerProperty ageProperty = BlockStateProperties.AGE_15;
    public static Supplier<ArcaneFireBlock> instance;
    private final MapCodec<ArcaneFireBlock> codec = MapCodec.unit(this);

    public ArcaneFireBlock(BlockBehaviour.Properties properties) {
        super(properties
                .noCollision()
                .instabreak()
                .replaceable()
                .lightLevel(state -> 8)
                .sound(net.minecraft.world.level.block.SoundType.WOOL)
                .noOcclusion()
                .noLootTable()
                .pushReaction(PushReaction.DESTROY), 0);

        registerDefaultState(stateDefinition.any().setValue(ageProperty, 0));
    }

    @Override
    protected MapCodec<? extends BaseFireBlock> codec() {
        return codec;
    }

    public static void spawnDelayed(ServerLevel level, BlockPos blockPos, Vec3 origin) {
        TetraMod.packetHandler.sendToAllPlayersNear(new SpawnParticlesPacket(
                        origin.x(), origin.y(), origin.z(),
                        blockPos.getX() + 0.5f, blockPos.getY(), blockPos.getZ() + 0.5f, false, 8,
                        Particles.splinteredPower.get()),
                level, blockPos, 64);

        ServerScheduler.schedule(40, () -> spawnDust(level, blockPos, 0.5f));
        ServerScheduler.schedule(38, () -> level.playSound(null, blockPos, TetraSounds.arcane_fire_1, SoundSource.PLAYERS, 0.05f, 1));
        ServerScheduler.schedule(55, () -> spawnDust(level, blockPos, 0.2f));
        ServerScheduler.schedule(52, () -> level.playSound(null, blockPos, TetraSounds.arcane_fire_2, SoundSource.PLAYERS, 0.075f, 1));

        level.playSound(null, blockPos, TetraSounds.destabilize, SoundSource.PLAYERS, 0.4f, 1.2f);
        ServerScheduler.schedule(80, () -> level.playSound(null, blockPos, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.2f, 1f));

        ServerScheduler.schedule(80, () -> level.setBlock(blockPos, ArcaneFireBlock.instance.get().defaultBlockState(),
                Block.UPDATE_ALL));
    }

    private static void spawnDust(Level level, BlockPos pos, float spread) {
        RandomSource random = level.getRandom();
        ((ServerLevel) level).sendParticles(
                new DustColorTransitionOptions(ARGB.color(255, 128, 185), ARGB.color(255, 188, 147), 0.8f + random.nextFloat() * 0.4f),
                pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f,
                8, spread, spread, spread, 0.1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ageProperty);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        // Check if water is flowing into this block. The extinguish sound this used to play
        // cannot happen here any more, the level is read only in a shape update now.
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && fluidState.getType() == Fluids.WATER) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    private void drainOrExtinguish(BlockState state, LevelAccessor level, BlockPos pos) {
        int age = state.getValue(ageProperty);
        if (age < BlockStateProperties.MAX_AGE_15) {
            level.setBlock(pos, state.setValue(ageProperty, age + 1), 3);
        } else {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.8F);
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool, boolean willHarvest,
            FluidState fluid) {
        int factor = BlockStateProperties.MAX_AGE_15 - state.getValue(ageProperty) + 1;
        UnstablePowerMobEffect.addOrUpdate(player, factor * 20, 0);
        if (!level.isClientSide()) {
            Particles.addSputteringPower((ServerLevel) level, pos.getX() + 0.5f, pos.getY() + 0.2f, pos.getZ() + 0.5f, player, Math.max(4, factor));
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean flag) {
        entity.hurt(level.damageSources().inFire(), 0.5f);

        if (!level.isClientSide() && level.getGameTime() % 10 == 0 && entity instanceof LivingEntity livingEntity) {
            UnstablePowerMobEffect.addOrUpdate(livingEntity, 30, level.getRandom().nextFloat() < 0.04f ? 1 : 0);
            drainOrExtinguish(state, level, pos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, getFireTickDelay(random));

        if (!canSurvive(state, level, pos)) {
            level.removeBlock(pos, false);
            return;
        }

        drainOrExtinguish(state, level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, getFireTickDelay(level.getRandom()));
    }

    private static int getFireTickDelay(RandomSource random) {
        return 40 + random.nextInt(40);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (random.nextInt(24) == 0) {
            level.playLocalSound(x + 0.5, y + 0.5, z + 0.5,
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6f + random.nextFloat() * 0.4f,
                    random.nextFloat() * 0.7f + 0.5f, false);
        }

        for (int i = 0; i < 4; ++i) {
            level.addParticle(new DustColorTransitionOptions(ARGB.color(255, 128, 185), ARGB.color(255, 188, 147),
                            0.8f + random.nextFloat() * 0.4f),
                    x + random.nextDouble(), y + random.nextDouble() * 0.5 + 0.5, z + random.nextDouble(), 0, 0, 0);
        }

        level.addParticle(Particles.arcaneFire.get(),
                x + 0.5f + random.nextGaussian() * 0.7f, y, z + 0.5 + random.nextGaussian() * 0.7f,
                x + 0.5, y + random.nextDouble(), z + 0.5f);
    }
}
