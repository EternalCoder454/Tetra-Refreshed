package se.mickelus.tetra.module.schematic.requirement;

import net.minecraft.network.chat.Component;
import se.mickelus.tetra.module.schematic.CraftingContext;

import javax.annotation.Nullable;
import java.util.List;

public interface CraftingRequirement {
    public boolean test(CraftingContext context);

    @Nullable
    default List<Component> getDescription() {
        return List.of(Component.translatable("tetra.holo.unknown_requirement"));
    }

    public static class AnyRequirement implements CraftingRequirement {
        public boolean test(CraftingContext context) {
            return true;
        }

        @Nullable
        public List<Component> getDescription() {
            return null;
        }
    }
}
