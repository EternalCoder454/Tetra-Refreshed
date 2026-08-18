package se.mickelus.tetra.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.ItemAbility;
import se.mickelus.tetra.effect.EffectHelper;
import se.mickelus.tetra.properties.IToolProvider;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class FortuneBonusCondition implements LootItemCondition {
    public static final String identifier = "random_chance_with_fortune";
    public static final MapCodec<FortuneBonusCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("chance").forGetter(condition -> condition.chance),
            Codec.FLOAT.fieldOf("fortuneMultiplier").forGetter(condition -> condition.fortuneMultiplier),
            ItemAbility.CODEC.optionalFieldOf("requiredTool").forGetter(condition -> Optional.ofNullable(condition.requiredTool)),
            Codec.INT.optionalFieldOf("requiredToolLevel", -1).forGetter(condition -> condition.requiredToolLevel)
    ).apply(instance, (chance, fortuneMultiplier, requiredTool, requiredToolLevel) ->
            new FortuneBonusCondition(chance, fortuneMultiplier, requiredTool.orElse(null), requiredToolLevel)));

    private final int requiredToolLevel;
    private final float chance;
    private final float fortuneMultiplier;
    private final ItemAbility requiredTool;

    public FortuneBonusCondition(float chance, float fortuneMultiplier, ItemAbility requiredTool, int requiredToolLevel) {
        this.chance = chance;
        this.fortuneMultiplier = fortuneMultiplier;
        this.requiredTool = requiredTool;
        this.requiredToolLevel = requiredToolLevel;
    }

    @Override
    public boolean test(LootContext context) {
        int fortuneLevel = 0;
        ItemStack toolStack = context.getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack stack ? stack : null;

        if (toolStack != null) {
            if (requiredTool == null) {
                fortuneLevel = EffectHelper.getEnchantmentLevel(Enchantments.FORTUNE, toolStack);
            } else if (toolStack.getItem() instanceof IToolProvider toolProvider
                    && toolProvider.getToolLevel(toolStack, requiredTool) > requiredToolLevel) {
                fortuneLevel = EffectHelper.getEnchantmentLevel(Enchantments.FORTUNE, toolStack);
            }
        }

        return context.getRandom().nextFloat() < this.chance + fortuneLevel * this.fortuneMultiplier;
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return CODEC;
    }
}
