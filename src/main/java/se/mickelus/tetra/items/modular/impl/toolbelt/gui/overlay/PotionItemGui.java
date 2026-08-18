package se.mickelus.tetra.items.modular.impl.toolbelt.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.GuiColors;
import se.mickelus.tetra.gui.GuiTextures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PotionItemGui extends GuiElement {
    private final ItemStack itemStack;
    private final int slot;
    private final Minecraft mc;
    private final KeyframeAnimation showAnimation;
    GuiTexture backdrop;

    public PotionItemGui(int x, int y, ItemStack itemStack, int slot, boolean animateUp) {
        super(x, y, 23, 23);

        setAttachmentPoint(GuiAttachment.middleLeft);
        setAttachmentAnchor(GuiAttachment.middleLeft);

        this.itemStack = itemStack;
        this.slot = slot;

        mc = Minecraft.getInstance();

        backdrop = new GuiTexture(0, 0, 23, 23, 32, 28, GuiTextures.toolbelt);
        addChild(backdrop);

        isVisible = false;
        showAnimation = new KeyframeAnimation(80, this)
                .applyTo(new Applier.TranslateY(animateUp ? y + 2 : y - 2, y), new Applier.Opacity(0, 1))
                .withDelay((int) (Math.random() * 300));
    }

    @Override
    protected void onShow() {
        showAnimation.start();
    }

    @Override
    protected boolean onHide() {
        if (showAnimation.isActive()) {
            showAnimation.stop();
        }
        return true;
    }

    @Override
    public void draw(GuiGraphicsExtractor graphics, int refX, int refY, int screenWidth, int screenHeight, int mouseX, int mouseY, float opacity) {
        super.draw(graphics, refX, refY, screenWidth, screenHeight, mouseX, mouseY, opacity);

        if (this.opacity == 1) {
            drawItemStack(graphics, itemStack, x + refX + 3, y + refY + 2);
        }
    }

    private void drawItemStack(GuiGraphicsExtractor graphics, ItemStack itemStack, int x, int y) {

        graphics.item(itemStack, x, y);
        graphics.renderItemDecorations(mc.font, itemStack, x, y, "");
    }


    public int getSlot() {
        return slot;
    }

    @Override
    protected void onFocus() {
        backdrop.setColor(GuiColors.hover);
    }

    @Override
    protected void onBlur() {
        backdrop.setColor(GuiColors.normal);
    }

    @Override
    public void updateFocusState(int refX, int refY, int mouseX, int mouseY) {
        mouseX -= refX + x;
        mouseY -= refY + y;
        boolean gainFocus = mouseX + mouseY >= 12;

        if (mouseX + mouseY > 34) {
            gainFocus = false;
        }

        if (mouseX - mouseY > 8) {
            gainFocus = false;
        }

        if (mouseY - mouseX > 12) {
            gainFocus = false;
        }

        if (gainFocus != hasFocus) {
            hasFocus = gainFocus;
            if (hasFocus) {
                onFocus();
            } else {
                onBlur();
            }
        }
    }
}
