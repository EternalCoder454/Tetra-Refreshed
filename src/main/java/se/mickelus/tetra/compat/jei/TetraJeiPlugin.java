package se.mickelus.tetra.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.forged.hammer.HammerBaseBlock;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;
import se.mickelus.tetra.compat.viewer.TetraViewerContent;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;

/**
 * Surfaces Tetra's materials in JEI.
 *
 * JEI finds this by the annotation, so nothing outside this package refers to it and a pack without
 * JEI never loads it. The content comes from se.mickelus.tetra.compat.viewer, which knows nothing
 * about JEI, so a second viewer is a second class in this shape rather than a second extraction.
 */
@JeiPlugin
public class TetraJeiPlugin implements IModPlugin {
    private static final Identifier uid = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MaterialCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MaterialCategory.type, TetraViewerContent.materials());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // The three places a player is holding the question this page answers.
        registration.addRecipeCatalyst(BasicWorkbenchBlock.instance, MaterialCategory.type);
        registration.addRecipeCatalyst(HammerBaseBlock.instance, MaterialCategory.type);
        registration.addRecipeCatalyst(ModularHolosphereItem.instance, MaterialCategory.type);
    }
}
