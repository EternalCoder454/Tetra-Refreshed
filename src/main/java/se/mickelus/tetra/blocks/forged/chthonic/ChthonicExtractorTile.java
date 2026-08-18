package se.mickelus.tetra.blocks.forged.chthonic;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class ChthonicExtractorTile extends BlockEntity {
    private static final String damageKey = "dmg";
    public static Supplier<BlockEntityType<ChthonicExtractorTile>> type;
    private int damage = 0;

    public ChthonicExtractorTile(BlockPos p_155268_, BlockState p_155269_) {
        super(type.get(), p_155268_, p_155269_);
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
        setChanged();
    }

    public void damage(int amount) {
        int newDamage = getDamage() + amount;

        if (newDamage < ChthonicExtractorBlock.maxDamage) {
            setDamage(newDamage);
        } else if (level != null) {
            level.levelEvent(null, 2001, getBlockPos(), Block.getId(level.getBlockState(getBlockPos())));
            level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        damage = input.getIntOr(damageKey, damage);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(damageKey, damage);
    }
}
