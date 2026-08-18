package se.mickelus.tetra.items.modular.impl;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import se.mickelus.mutil.util.CastOptional;
import se.mickelus.tetra.items.modular.ItemModularHandheld;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockProgressOverlay implements GuiLayer {
    private final Minecraft mc;

    private final GuiBlockProgress gui;

    public BlockProgressOverlay(Minecraft mc) {
        this.mc = mc;

        gui = new GuiBlockProgress(mc);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        if (mc.player != null) {
            ItemStack activeStack = mc.player.getUseItem();

            gui.setProgress(
                    CastOptional.cast(activeStack.getItem(), ItemModularHandheld.class)
                            .map(item -> item.getBlockProgress(activeStack, mc.player))
                            .orElse(0f));
        }
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        this.gui.draw(guiGraphics);
    }
}
