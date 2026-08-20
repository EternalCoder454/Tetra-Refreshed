package se.mickelus.tetra.items.modular.impl.holo;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;

/**
 * Opens the holosphere screen.
 *
 * This lived on ModularHolosphereItem behind an @OnlyIn(Dist.CLIENT). NeoForge does not strip
 * @OnlyIn from mod classes, so the item still carried a reference to a Screen subclass and loading
 * it on a dedicated server threw NoClassDefFoundError before any item could register. Keeping the
 * screen behind its own class means the server never loads it, which is what @OnlyIn was being
 * asked to do and cannot.
 */
public class HolosphereGuiOpener {
    private HolosphereGuiOpener() {
    }

    public static void showGui() {
        HoloGui gui = HoloGui.getInstance();

        Minecraft.getInstance().setScreen(gui);
        gui.onShow();
    }
}
