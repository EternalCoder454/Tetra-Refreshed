package se.mickelus.tetra.blocks.scroll;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import se.mickelus.tetra.TetraRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.core.component.DataComponentGetter;

@ParametersAreNonnullByDefault
public class ScrollTile extends BlockEntity {
    public static final String identifier = "scroll";
    public static BlockEntityType<ScrollTile> type;

    private ScrollData[] scrolls = new ScrollData[0];

    public ScrollTile(BlockPos p_155268_, BlockState p_155269_) {
        super(type, p_155268_, p_155269_);
    }

    public ScrollData[] getScrolls() {
        return scrolls;
    }

    public boolean addScroll(ItemStack itemStack) {
        if (scrolls.length >= 6) {
            return false;
        }

        return ScrollData.readOptional(itemStack)
                .map(data -> {
                    scrolls = ArrayUtils.add(scrolls, data);
                    setChanged();
                    return true;
                })
                .orElse(false);
    }

    public Identifier[] getSchematics() {
        boolean isIntricate = isIntricate();
        return Arrays.stream(scrolls)
                .filter(data -> data.isIntricate == isIntricate)
                .map(data -> data.schematics)
                .flatMap(Collection::stream)
                .toArray(Identifier[]::new);
    }

    public Identifier[] getCraftingEffects() {
        boolean isIntricate = isIntricate();
        return Arrays.stream(scrolls)
                .filter(data -> data.isIntricate == isIntricate)
                .map(data -> data.craftingEffects)
                .flatMap(Collection::stream)
                .toArray(Identifier[]::new);
    }

    public boolean isIntricate() {
        return Arrays.stream(scrolls)
                .allMatch(data -> data.isIntricate);
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        scrolls = ScrollData.read(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ScrollData.write(scrolls, output);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentInput) {
        super.applyImplicitComponents(componentInput);
        ScrollData data = componentInput.get(TetraRegistries.scrollData.get());
        if (data != null) {
            scrolls = new ScrollData[]{data};
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (scrolls.length == 1) {
            components.set(TetraRegistries.scrollData.get(), scrolls[0]);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("data");
    }
}
