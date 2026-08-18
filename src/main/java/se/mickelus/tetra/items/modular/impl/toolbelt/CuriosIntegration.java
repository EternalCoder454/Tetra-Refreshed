package se.mickelus.tetra.items.modular.impl.toolbelt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Reads a toolbelt out of the player's curios slots.
 *
 * Curios is an optional dependency and its api is compileOnly, so on a pack without it the class is
 * simply absent. Touching CuriosApi from ToolbeltHelper meant that class failed to load the moment a
 * player ticked, which crashed the game on the first tick after a world loaded.
 *
 * The api reference sits in the nested Access class rather than here, so that resolving this class
 * never pulls CuriosApi in. Access is loaded the first time one of its methods runs, which only
 * happens once isLoaded has said curios is there.
 */
public class CuriosIntegration {
    private static final boolean loaded = ModList.get().isLoaded("curios");

    private CuriosIntegration() {
    }

    public static Optional<ItemStack> findToolbelt(Player player) {
        if (!loaded) {
            return Optional.empty();
        }

        return Access.findToolbelt(player);
    }

    private static class Access {
        static Optional<ItemStack> findToolbelt(Player player) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.findFirstCurio(ModularToolbeltItem.instance.get()))
                    .map(slotResult -> slotResult.stack());
        }
    }
}
