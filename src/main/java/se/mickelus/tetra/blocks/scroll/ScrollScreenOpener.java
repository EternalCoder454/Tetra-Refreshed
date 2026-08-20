package se.mickelus.tetra.blocks.scroll;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.blocks.scroll.gui.ScrollScreen;

/**
 * Opens the scroll details screen.
 *
 * This was a method on ScrollItem behind an @OnlyIn(Dist.CLIENT). NeoForge does not strip @OnlyIn
 * from mod classes, so the item kept a reference to a Screen subclass and could not load on a
 * dedicated server. Keeping the screen behind its own class is what @OnlyIn was being asked to do
 * and cannot.
 */
public class ScrollScreenOpener {
    private ScrollScreenOpener() {
    }

    public static void showDetails(String detailsKey) {
        Minecraft.getInstance().setScreen(new ScrollScreen(detailsKey));
    }
}
