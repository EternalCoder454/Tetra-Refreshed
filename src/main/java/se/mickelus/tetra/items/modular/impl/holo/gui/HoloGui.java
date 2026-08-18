package se.mickelus.tetra.items.modular.impl.holo.gui;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.tetra.ConfigHandler;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.gui.GuiSpinner;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.HoloPage;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloCraftRootGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.scan.HoloScanRootGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.system.HoloSystemRootGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.input.CharacterEvent;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class HoloGui extends Screen {
    private static final Logger logger = LogManager.getLogger();
    private static HoloGui instance = null;
    private static boolean hasListener = false;
    private final HoloHeaderGui header;
    private final HoloRootBaseGui[] pages;
    private final GuiElement defaultGui;
    private final GuiElement spinner;
    private HoloRootBaseGui currentPage;
    private Runnable closeCallback;

    public HoloGui() {
        super(Component.literal("tetra:holosphere"));

        width = 320;
        height = 240;

        // fontRenderer = Minecraft.getInstance().fontRenderer;
        defaultGui = new GuiElement(0, 0, width, height);

        header = new HoloHeaderGui(0, 0, width, this::changePage);
        defaultGui.addChild(header);

        pages = new HoloRootBaseGui[HoloPage.values().length];
        pages[0] = new HoloCraftRootGui(0, 18);
        defaultGui.addChild(pages[0]);
        pages[1] = new HoloScanRootGui(0, 18);
        pages[1].setVisible(false);
        defaultGui.addChild(pages[1]);
        pages[2] = new HoloSystemRootGui(0, 18);
        pages[2].setVisible(false);
        defaultGui.addChild(pages[2]);

        currentPage = pages[0];

        spinner = new GuiSpinner(-8, 6);
        spinner.setVisible(false);
        defaultGui.addChild(spinner);

        if (ConfigHandler.development.get() && !hasListener) {
            DataManager.instance.itemEffectData.onReload(() -> {
                Minecraft.getInstance().executeBlocking(HoloGui::onReload);
            });
            hasListener = true;
        }
    }

    public static HoloGui getInstance() {
        if (instance == null) {
            instance = new HoloGui();
        }

        return instance;
    }

    private static void onReload() {
        if (instance != null && instance.getMinecraft().screen == instance) {
            logger.info("Refreshing holosphere gui data");
            instance.spinner.setVisible(false);
            if (instance.currentPage != null) {
                instance.currentPage.onReload();
            }
        }
    }

    public void openSchematic(IModularItem item, ItemStack itemStack, String slot, UpgradeSchematic schematic, Runnable closeCallback) {
        changePage(HoloPage.craft);

        ((HoloCraftRootGui) pages[0]).openFromWorkbench(item, itemStack, slot, schematic);
        this.closeCallback = closeCallback;
    }

    @Override
    public void removed() {
        super.removed();
        if (closeCallback != null) {
            // onClose is called in Minecarft.displayGuiScreen, pre-null-assignement prevents gui chaining from getting stuck in recursion
            Runnable callback = closeCallback;
            this.closeCallback = null;
            callback.run();
        }
    }

    public void onShow() {
        header.onShow();
        currentPage.animateOpen();
    }

    private void changePage(HoloPage page) {
        header.changePage(page);

        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisible(page.ordinal() == i);
        }

        currentPage = pages[page.ordinal()];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        defaultGui.updateFocusState((width - defaultGui.getWidth()) / 2, (height - defaultGui.getHeight()) / 2, mouseX, mouseY);
        defaultGui.draw(graphics, (width - defaultGui.getWidth()) / 2, (height - defaultGui.getHeight()) / 2,
                width, height, mouseX, mouseY, 1);

        renderHoveredToolTip(graphics, mouseX, mouseY);
    }

    protected void renderHoveredToolTip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> tooltipLines = defaultGui.getTooltipLines();
        if (tooltipLines != null) {
            graphics.setTooltipForNextFrame(font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (defaultGui.onMouseClick((int) event.x(), (int) event.y(), event.button())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentPage.onMouseScroll(mouseX, mouseY, scrollY)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (currentPage.onKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
        if (currentPage.onKeyRelease(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char typedChar = (char) event.codepoint();
        if (currentPage.onCharType(typedChar, event.codepoint())) {
            return true;
        }

        if (ConfigHandler.development.get()) {
            switch (typedChar) {
                case 'r':
                    instance = null;
                    Minecraft.getInstance().setScreen(null);

                    HoloGui gui = HoloGui.getInstance();
                    Minecraft.getInstance().setScreen(gui);
                    gui.onShow();
                    break;
                case 't':
                    getMinecraft().player.connection.sendCommand("reload");
                    spinner.setVisible(true);
                    break;
            }
        }

        return false;
    }
}
