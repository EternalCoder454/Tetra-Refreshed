package se.mickelus.tetra.blocks.forged.chthonic;

import se.mickelus.tetra.blocks.TetraBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.level.block.state.BlockBehaviour;

@ParametersAreNonnullByDefault
public class DepletedBedrockBlock extends TetraBlock {
    public static final String identifier = "depleted_bedrock";

    public static DepletedBedrockBlock instance;

    public DepletedBedrockBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(-1.0F, 3600000.0F).noLootTable());
    }
}
