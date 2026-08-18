package se.mickelus.tetra.effect.data.provider.number;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import se.mickelus.tetra.effect.data.ItemEffectContext;
import se.mickelus.tetra.effect.data.provider.entity.EntityProvider;

public class EntityPropertyNumberProvider implements NumberProvider {
    EntityProvider entity;
    EntityProperty property;

    @Override
    public float getValue(ItemEffectContext context) {
        Entity resolvedEntity = entity.getEntity(context);
        return switch (property) {
            case freezingTicks -> resolvedEntity.getTicksFrozen();
            case frozenLimit -> resolvedEntity.getTicksRequiredToFreeze();
            case burningDuration -> resolvedEntity.getRemainingFireTicks();
            case airSupply -> resolvedEntity.getAirSupply();
            case maxAirSupply -> resolvedEntity.getMaxAirSupply();
            case fallDistance -> (float) resolvedEntity.fallDistance;
            case health -> resolvedEntity instanceof LivingEntity livingEntity ? livingEntity.getHealth() : 0;
            case maxHealth -> resolvedEntity instanceof LivingEntity livingEntity ? livingEntity.getMaxHealth() : 0;
            case armor -> resolvedEntity instanceof LivingEntity livingEntity ? livingEntity.getArmorValue() : 0;
            case absorption -> resolvedEntity instanceof LivingEntity livingEntity ? livingEntity.getAbsorptionAmount() : 0;
        };
    }

    enum EntityProperty {
        freezingTicks,
        frozenLimit,
        burningDuration,
        airSupply,
        maxAirSupply,
        fallDistance,
        health,
        maxHealth,
        armor,
        absorption,
    }
}
