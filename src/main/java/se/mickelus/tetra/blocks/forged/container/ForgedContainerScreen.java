package se.mickelus.tetra.blocks.forged.container;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiRect;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.mutil.gui.animation.AnimationChain;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.gui.GuiTextures;
import se.mickelus.tetra.gui.VerticalTabGroupGui;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class ForgedContainerScreen extends AbstractContainerScreen<ForgedContainerMenu> {
    private static final Identifier containerTexture = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/forged-container.png");

    private final ForgedContainerBlockEntity tileEntity;
    private final ForgedContainerMenu container;

    private final GuiElement guiRoot;

    private final AnimationChain slotTransition;

    private final VerticalTabGroupGui compartmentButtons;
    private boolean guiHandledMouseDown = false;

    public ForgedContainerScreen(ForgedContainerMenu container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title, 179, 176);

        this.tileEntity = container.getTile();
        this.container = container;

        guiRoot = new GuiElement(0, 0, imageWidth, imageHeight);
        guiRoot.addChild(new GuiTexture(0, -13, 179, 128, containerTexture));
        guiRoot.addChild(new GuiTexture(0, 103, 179, 106, GuiTextures.playerInventory));

        compartmentButtons = new VerticalTabGroupGui(10, 26, this::changeCompartment, containerTexture, 0, 128,
                IntStream.range(0, ForgedContainerBlockEntity.compartmentCount)
                        .mapToObj(i -> I18n.get("tetra.forged_container.compartment_" + i))
                        .toArray(String[]::new));
        guiRoot.addChild(compartmentButtons);

        GuiRect slotTransitionElement = new GuiRect(12, 0, 152, 101, 0);
        slotTransitionElement.setOpacity(0);
        guiRoot.addChild(slotTransitionElement);
        slotTransition = new AnimationChain(
                new KeyframeAnimation(30, slotTransitionElement).applyTo(new Applier.Opacity(0.3f)),
                new KeyframeAnimation(50, slotTransitionElement).applyTo(new Applier.Opacity(0)));
    }

    private void changeCompartment(int index) {
        container.changeCompartment(index);
        compartmentButtons.setActive(index);
        slotTransition.stop();
        slotTransition.start();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        guiRoot.updateFocusState(this.leftPos, this.topPos, (int) event.x(), (int) event.y());
        guiHandledMouseDown = guiRoot.onMouseClick((int) event.x(), (int) event.y(), event.button());
        if (guiHandledMouseDown) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        guiRoot.onMouseRelease((int) event.x(), (int) event.y(), event.button());
        if (guiHandledMouseDown) {
            guiHandledMouseDown = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char typecChar = (char) event.codepoint();
        if (compartmentButtons.keyTyped(typecChar)) {
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int size = ForgedContainerBlockEntity.compartmentSize;
        var itemHandler = tileEntity.getItemHandler(null);
        if (itemHandler != null) {
            for (int i = 0; i < ForgedContainerBlockEntity.compartmentCount; i++) {
                boolean hasContent = false;
                for (int j = 0; j < size; j++) {
                    if (!itemHandler.getStackInSlot(i * size + j).isEmpty()) {
                        hasContent = true;
                        break;
                    }
                }
                compartmentButtons.setHasContent(i, hasContent);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiRoot.updateFocusState(x, y, mouseX, mouseY);
        guiRoot.draw(graphics, x, y, width, height, mouseX, mouseY, 1);
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

    }
}
