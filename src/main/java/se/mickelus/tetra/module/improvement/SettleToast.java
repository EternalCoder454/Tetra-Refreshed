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
import se.mickelus.mutil.util.CastOptional;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.TetraSounds;
import se.mickelus.tetra.blocks.workbench.gui.GuiModuleGlyph;
import se.mickelus.tetra.gui.GuiColors;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.schematic.SchematicRarity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class SettleToast implements Toast {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/toasts.png");
    private final ItemStack itemStack;
    private final String moduleName;
    private final GuiModuleGlyph glyph;
    private boolean hasPlayedSound = false;

    public SettleToast(ItemStack itemStack, String slot) {
        this.itemStack = itemStack;

        ItemModule itemModule = CastOptional.cast(itemStack.getItem(), IModularItem.class)
                .map(item -> item.getModuleFromSlot(itemStack, slot))
                .orElse(null);

        glyph = Optional.ofNullable(itemModule)
                .map(module -> module.getVariantData(itemStack))
                .map(data -> data.glyph)
                .map(glyphData -> new GuiModuleGlyph(0, 0, 16, 16, glyphData).setShift(false))
                .orElse(null);

        moduleName = Optional.ofNullable(itemModule)
                .map(module -> module.getName(itemStack))
                .orElse(slot);
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
            toastManager.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(TetraSounds.settle, 1, 1));
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

        if (glyph != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 20, 14, 160, 0, 15, 15, 256, 256);
            glyph.draw(graphics, 19, 14, 260, 43, -1, -1, 1);
        }

        graphics.text(font, I18n.get(TetraMod.MOD_ID + ".settled.toast"), 30, 7, SchematicRarity.hone.tint);
        graphics.text(font, font.plainSubstrByWidth(moduleName, 118), 37, 18, GuiColors.muted);

        graphics.item(itemStack, 8, 8);
        graphics.itemDecorations(font, itemStack, 8, 8);
    }
}
