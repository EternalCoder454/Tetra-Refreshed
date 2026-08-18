package se.mickelus.tetra.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;
import se.mickelus.tetra.compat.viewer.MaterialSummary;
import se.mickelus.tetra.compat.viewer.ViewerMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows one material: what counts as it, and what it contributes to a module made from it.
 *
 * The slot holds every item that resolves to the material, so asking what an iron ingot is used for
 * finds this page. That lookup is the point, and it is the question the holosphere answers slowly.
 */
@OnlyIn(Dist.CLIENT)
public class MaterialCategory extends AbstractRecipeCategory<ViewerMaterial> {
    public static final RecipeType<ViewerMaterial> type = RecipeType.create(TetraMod.MOD_ID, "material", ViewerMaterial.class);

    private static final int width = 160;
    private static final int height = 108;
    private static final int textLeft = 28;
    private static final int margin = 5;

    public MaterialCategory(IGuiHelper guiHelper) {
        super(type,
                Component.translatable("tetra.jei.material.title"),
                guiHelper.createDrawableItemLike(BasicWorkbenchBlock.instance),
                width, height);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ViewerMaterial material, IFocusGroup focuses) {
        // INPUT rather than a crafting station, so that asking what an iron ingot is used for finds
        // this page. Nothing is consumed, but that lookup is the whole point of the page.
        builder.addSlot(RecipeIngredientRole.INPUT, margin, margin)
                .setStandardSlotBackground()
                .addItemStacks(material.items());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ViewerMaterial material, IFocusGroup focuses) {
        builder.addText(new ArrayList<FormattedText>(MaterialSummary.lines(material)), width - textLeft - margin, height - margin * 2)
                .setPosition(textLeft, margin);
    }

    @Override
    public Identifier getIdentifier(ViewerMaterial material) {
        return Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "material/" + material.key());
    }
}
