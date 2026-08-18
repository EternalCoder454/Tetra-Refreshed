package se.mickelus.tetra.effect.data.provider.number;

import net.minecraft.nbt.CompoundTag;
import se.mickelus.tetra.effect.data.ItemEffectContext;
import se.mickelus.tetra.effect.data.provider.entity.EntityProvider;

public class EntityDataNumberProvider implements NumberProvider {
    EntityProvider entity;
    String key;
    int defaultValue = -1;

    @Override
    public float getValue(ItemEffectContext context) {
        CompoundTag data = entity.getEntity(context).getPersistentData();
        if (data.contains(key)) {
            return data.getFloatOr(key, 0.0F);
        }
        return defaultValue;
    }
}
