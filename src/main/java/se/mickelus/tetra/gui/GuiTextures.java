package se.mickelus.tetra.gui;

import net.minecraft.resources.Identifier;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GuiTextures {
    public static final Identifier workbench = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/workbench.png");
    public static final Identifier holo = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/holo.png");
    public static final Identifier toolActions = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/tool-actions.png");
    public static final Identifier playerInventory = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/player-inventory.png");
    public static final Identifier toolbelt = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/toolbelt-inventory.png");
    public static final Identifier glyphs = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/glyphs.png");
    public static final Identifier hud = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/hud.png");
}
