package se.mickelus.tetra.effect.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

public class EffectUnRenderer implements IClientMobEffectExtensions {
    public static final IClientMobEffectExtensions INSTANCE = new EffectUnRenderer();

    @Override
    public boolean renderInventoryIcon(MobEffectInstance instance, AbstractContainerScreen<?> screen, GuiGraphicsExtractor guiGraphics, int x, int y, int blitOffset) {
        return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphicsExtractor guiGraphics, int x, int y, float z, float alpha) {
        return true;
    }

    @Override
    public boolean isVisibleInGui(MobEffectInstance instance) {
        return false;
    }

    @Override
    public boolean isVisibleInInventory(MobEffectInstance instance) {
        return false;
    }
}
