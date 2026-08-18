package se.mickelus.tetra.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.compat.viewer.MaterialSummary;
import se.mickelus.tetra.compat.viewer.ViewerMaterial;

import java.util.List;

/**
 * One material page, as EMI draws it. The same content the JEI category shows, laid out with EMI's
 * widgets rather than its own text.
 */
@OnlyIn(Dist.CLIENT)
public class MaterialEmiRecipe extends BasicEmiRecipe {
    private static final int panelWidth = 160;
    private static final int panelHeight = 108;
    private static final int textLeft = 28;
    private static final int margin = 5;
    private static final int lineHeight = 10;
    private static final int textColor = 0xffffff;

    private final ViewerMaterial material;

    public MaterialEmiRecipe(ViewerMaterial material) {
        super(TetraEmiPlugin.materialCategory,
                Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "material/" + material.key()),
                panelWidth, panelHeight);
        this.material = material;

        // An input rather than a catalyst, so that asking what an iron ingot is used for finds this
        // page. Nothing is consumed, but that lookup is the whole point of the page.
        this.inputs = List.of(EmiIngredient.of(material.items().stream().map(EmiStack::of).toList()));
        this.outputs = List.of();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), margin, margin);

        // EMI places each line itself rather than taking a block of text, so the styled components
        // are drawn one per row. Shadowed, because EMI's own recipe text is.
        List<Component> lines = MaterialSummary.lines(material);
        for (int i = 0; i < lines.size(); i++) {
            widgets.addText(lines.get(i), textLeft, margin + i * lineHeight, textColor, true);
        }
    }

    /**
     * A material is a description rather than something craftable, so it has no place in the recipe
     * tree and should never be shown as something the player could make.
     */
    @Override
    public boolean supportsRecipeTree() {
        return false;
    }
}
