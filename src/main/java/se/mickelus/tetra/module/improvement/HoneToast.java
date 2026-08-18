package se.mickelus.tetra.module.improvement;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.TetraSounds;
import se.mickelus.tetra.gui.GuiColors;
import se.mickelus.tetra.module.schematic.SchematicRarity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HoneToast implements Toast {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/toasts.png");
    private final ItemStack itemStack;
    private boolean hasPlayedSound = false;

    public HoneToast(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    private Visibility visibility = Visibility.SHOW;

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long delta) {
        if (itemStack == null) {
            visibility = Visibility.HIDE;
            return;
        }

        if (!this.hasPlayedSound && delta > 0L) {
            toastManager.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(TetraSounds.honeGain, 1, 1));
            this.hasPlayedSound = true;
        }

        visibility = delta > 5000 ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long delta) {
        if (itemStack == null) {
            return;
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 160, 32, 256, 256);

        String itemName = font.plainSubstrByWidth(itemStack.getHoverName().getString(), 125);
        graphics.text(font, I18n.get("tetra.hone.available"), 30, 7, SchematicRarity.hone.tint);
        graphics.text(font, itemName, 30, 18, GuiColors.muted);

        graphics.item(itemStack, 8, 8);
        graphics.itemDecorations(font, itemStack, 8, 8);
    }
}
