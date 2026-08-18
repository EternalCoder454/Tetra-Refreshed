package se.mickelus.tetra.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import se.mickelus.mutil.gui.GuiElement;

public class ZOffsetGui extends GuiElement {
    protected double z;

    public ZOffsetGui(int x, int y, double z) {
        super(x, y, 0, 0);
        this.z = z;
    }

    @Override
    protected void drawChildren(GuiGraphicsExtractor graphics, int refX, int refY, int screenWidth, int screenHeight, int mouseX, int mouseY, float opacity) {
        // the gui transform is two dimensional now, so depth is a stratum rather than a z
        // offset. Anything drawn after this call lands above what came before it.
        graphics.nextStratum();
        super.drawChildren(graphics, refX, refY, screenWidth, screenHeight, mouseX, mouseY, opacity);
    }
}
