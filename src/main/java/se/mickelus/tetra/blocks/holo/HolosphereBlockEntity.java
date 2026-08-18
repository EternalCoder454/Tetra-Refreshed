package se.mickelus.tetra.blocks.holo;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import se.mickelus.tetra.ServerScheduler;
import se.mickelus.tetra.TetraSounds;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static se.mickelus.tetra.util.ItemStackTagHelper.setTag;

public class HolosphereBlockEntity extends BlockEntity {
    public static final int maxRange = 8;
    public static Supplier<BlockEntityType<HolosphereBlockEntity>> type;
    private List<ScanResult> scanResults;
    private long scanModeTimestamp = 0;

    private CompoundTag itemTag = new CompoundTag();

    // Null until something asks, and set back to null whenever the item changes. Working it out
    // means building a stack and reading its effects, which is far too much to do per tick.
    @Nullable
    private Boolean canScan;

    public HolosphereBlockEntity(BlockPos pos, BlockState blockState) {
        super(type.get(), pos, blockState);
        scanResults = new ArrayList<>();
    }

    public AABB getRenderBoundingBox() {
        return Shapes.block().bounds().inflate(1, 0.5, 1).move(worldPosition);
    }

    public List<ScanResult> getScanResults() {
        return scanResults;
    }

    public boolean canScan() {
        if (canScan == null) {
            canScan = resolveCanScan();
        }
        return canScan;
    }

    private boolean resolveCanScan() {
        ItemStack itemStack = new ItemStack(ModularHolosphereItem.instance);
        // A copy, because the stack would otherwise share the tag this block entity keeps.
        setTag(itemStack, itemTag != null ? itemTag.copy() : new CompoundTag());
        return Optional.ofNullable(ModularHolosphereItem.instance.getEffectData(itemStack))
                .map(effects -> effects.getLevel(ItemEffect.percussionScanner) > 0)
                .orElse(false);
    }

    public long getScanModeTimestamp() {
        return scanModeTimestamp;
    }

    public boolean inScanMode() {
        return scanModeTimestamp > 0;
    }

    public void toggleScanMode(boolean enable) {
        long diff = Math.abs(Math.abs(scanModeTimestamp) - level.getGameTime());
        long time = diff < 5 ? level.getGameTime() - diff : level.getGameTime();
        scanModeTimestamp = enable ? time : -time;

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private String[] getScannableStructures() {
        return new String[] { "#tetra:forged_ruins" };
    }

    private long getTimestamp(long gametime, int x, int y) {
        return (long) (gametime + (Mth.length(x, y) * 1.5) + Math.random() * 5);
    }

    public void use(int hammerLevel, float hammerEfficiency, float angle) {
        ServerLevel serverLevel = (ServerLevel) getLevel();
        int count = 4 + (int) hammerEfficiency / 4;
        double cone = (20 + hammerLevel * 6) * Math.PI / 180;
        int ox = SectionPos.blockToSectionCoord(getBlockPos().getX());
        int oz = SectionPos.blockToSectionCoord(getBlockPos().getZ());

        int preResultSize = scanResults.size();
        AtomicInteger stagger = new AtomicInteger();

        ChunkPos.rangeClosed(new ChunkPos(-32, -32), new ChunkPos(32, 32))
                .filter(pos -> Math.abs(Mth.atan2(pos.x(), pos.z()) - angle) < cone)
                .sorted(Comparator.comparingInt(pos -> pos.x() * pos.x() + pos.z() * pos.z()))
                .map(pos -> new ChunkPos(pos.x() + ox, pos.z() + oz))
                .filter(pos -> Math.abs(pos.x() - ox) + Math.abs(pos.z() - oz) < 20)
                .filter(pos -> scanResults.stream().noneMatch(result -> result.chunkX == pos.x() && result.chunkZ == pos.z()))
                .limit(count)
                .forEach(pos -> {
//                    boolean wasLoaded = serverLevel.hasChunk(pos.x, pos.z);
//                    System.out.println("has chunk [" + pos.x + ", " + pos.z + "]: " + wasLoaded);
//                    serverLevel.getChunkSource().getGenerator().findNearestMapStructure()
                    long timestamp = this.getTimestamp(serverLevel.getGameTime(), pos.x() - ox, pos.z() - oz) + stagger.getAndIncrement() * 3L;
                    int height = serverLevel.getChunk(pos.x(), pos.z(), ChunkStatus.SURFACE).getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.x(), pos.z());

                    BlockPos centerPos = pos.getMiddleBlockPosition(height);
                    float temperature = level.getBiome(centerPos).value().getBaseTemperature();
                    List<String> structures =
                            Arrays.stream(getScannableStructures())
                                    .filter(id -> ScanHelper.hasStructure(id, serverLevel, pos)).toList();
                    scanResults.add(new ScanResult(pos.x(), pos.z(), height, temperature, structures, timestamp));


//                    if (!wasLoaded) {
//                        System.out.println("[post]  Was not loaded, is now: " + serverLevel.hasChunk(pos.x, pos.z));
//                        ServerScheduler.schedule(100, () -> System.out.println("[delayed] Was not loaded, is now: " + serverLevel.hasChunk(pos.x, pos.z)));
//                    }


                    ServerScheduler.schedule((int) ((timestamp - serverLevel.getGameTime())), () ->
                            serverLevel.playSound(null, getBlockPos(), TetraSounds.scanMiss, SoundSource.PLAYERS, 0.01f, 1.0f + (float) Math.random() * 0f));
                    if (!structures.isEmpty()) {
                        ServerScheduler.schedule((int) ((timestamp - serverLevel.getGameTime()) + 20), () ->
                                serverLevel.playSound(null, getBlockPos(), TetraSounds.scanHit, SoundSource.PLAYERS, 0.1f, 1.0f));
                    }

                });

        if (preResultSize != scanResults.size()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getItemStack() {
        ItemStack itemStack = new ItemStack(ModularHolosphereItem.instance);
        setTag(itemStack, this.getItemTag());
        return itemStack;
    }

    public CompoundTag getItemTag() {
        return itemTag == null ? new CompoundTag() : itemTag.copy();
    }

    public void setItemTag(CompoundTag tag) {
        this.itemTag = tag == null ? new CompoundTag() : tag.copy();
        this.canScan = null;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        loadWithComponents(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        setItemTag(input.read("item", CompoundTag.CODEC).orElseGet(CompoundTag::new));

        scanModeTimestamp = input.getLongOr("timestamp", 0L);

        scanResults = input.listOrEmpty("scan", ScanResult.codec).stream()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.store("item", CompoundTag.CODEC, itemTag == null ? new CompoundTag() : itemTag.copy());

        output.putLong("timestamp", scanModeTimestamp);

        ValueOutput.TypedOutputList<ScanResult> list = output.list("scan", ScanResult.codec);
        scanResults.forEach(list::add);
    }

    record ScanResult(int chunkX, int chunkZ, int height, float temperature, List<String> structures, long timestamp) {
        static final Codec<ScanResult> codec = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("chunkX").forGetter(i -> i.chunkX),
                Codec.INT.fieldOf("chunkZ").forGetter(i -> i.chunkZ),
                Codec.INT.fieldOf("height").forGetter(i -> i.height),
                Codec.FLOAT.fieldOf("temperature").forGetter(i -> i.temperature),
                Codec.STRING.listOf().fieldOf("structures").forGetter(i -> i.structures),
                Codec.LONG.fieldOf("timestamp").forGetter(i -> i.timestamp)
        ).apply(instance, ScanResult::new));
    }
}
