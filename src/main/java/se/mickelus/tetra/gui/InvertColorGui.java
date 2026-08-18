package se.mickelus.tetra.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import se.mickelus.mutil.gui.GuiElement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class InvertColorGui extends GuiElement {

    public InvertColorGui(int x, int y) {
        super(x, y, 0, 0);
    }

    public InvertColorGui(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    protected void drawChildren(GuiGraphicsExtractor guiGraphics, int refX, int refY, int screenWidth, int screenHeight, int mouseX, int mouseY, float opacity) {

        // todo: doesn't handle opacity since render system changes in 1.18
        super.drawChildren(guiGraphics, refX, refY, screenWidth, screenHeight, mouseX, mouseY, opacity);
    }
}
