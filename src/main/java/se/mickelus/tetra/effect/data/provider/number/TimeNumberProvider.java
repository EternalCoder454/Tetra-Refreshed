package se.mickelus.tetra.effect.data.provider.number;

import se.mickelus.tetra.effect.data.ItemEffectContext;
import net.minecraft.world.attribute.EnvironmentAttributes;

public class TimeNumberProvider implements NumberProvider {
    TimeProperty property = TimeProperty.gameTime;

    @Override
    public float getValue(ItemEffectContext context) {
        return switch (property) {
            case gameTime -> context.getLevel().getGameTime();
            // Level#getDayTime became the dimension's own clock, and the moon phase became a
            // positional environment attribute rather than a level wide number.
            case dayTime -> context.getLevel().getDefaultClockTime();
            case moonPhase -> context.getLevel().environmentAttributes()
                    .getDimensionValue(EnvironmentAttributes.MOON_PHASE).index();
        };
    }

    enum TimeProperty {
        gameTime,
        dayTime,
        moonPhase,
    }
}
