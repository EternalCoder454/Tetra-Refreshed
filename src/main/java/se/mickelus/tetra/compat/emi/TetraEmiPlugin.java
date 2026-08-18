package se.mickelus.tetra.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.forged.hammer.HammerBaseBlock;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;
import se.mickelus.tetra.compat.viewer.TetraViewerContent;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;

/**
 * Surfaces Tetra's materials in EMI.
 *
 * EMI finds this by the annotation, so nothing outside this package refers to it and a pack without
 * EMI never loads it. The content comes from se.mickelus.tetra.compat.viewer, which knows nothing
 * about EMI, and the JEI plugin reads the same thing. That layer was written when EMI looked
 * impossible on this version, and adding this was the small class it was meant to make possible.
 */
@OnlyIn(Dist.CLIENT)
@EmiEntrypoint
public class TetraEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory materialCategory = new EmiRecipeCategory(
            Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "material"),
            EmiStack.of(BasicWorkbenchBlock.instance));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(materialCategory);

        // The three places a player is holding the question this page answers.
        registry.addWorkstation(materialCategory, EmiStack.of(BasicWorkbenchBlock.instance));
        registry.addWorkstation(materialCategory, EmiStack.of(HammerBaseBlock.instance));
        registry.addWorkstation(materialCategory, EmiStack.of(ModularHolosphereItem.instance));

        TetraViewerContent.materials().stream()
                .map(MaterialEmiRecipe::new)
                .forEach(registry::addRecipe);
    }
}
