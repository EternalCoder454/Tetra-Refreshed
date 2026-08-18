package se.mickelus.tetra.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;
import se.mickelus.tetra.compat.viewer.ViewerMaterial;
import se.mickelus.tetra.module.data.MaterialData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows one material: what counts as it, and what it contributes to a module made from it.
 *
 * The slot holds every item that resolves to the material, so asking what an iron ingot is used for
 * finds this page. That lookup is the point, and it is the question the holosphere answers slowly.
 *
 * The stat labels are the holosphere's own, so the page reads in the same words the mod already
 * uses rather than inventing a second vocabulary for the same numbers.
 */
@OnlyIn(Dist.CLIENT)
public class MaterialCategory extends AbstractRecipeCategory<ViewerMaterial> {
    public static final RecipeType<ViewerMaterial> type = RecipeType.create(TetraMod.MOD_ID, "material", ViewerMaterial.class);

    private static final String statPrefix = "tetra.holo.craft.materials.stat.";

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
        builder.addText(lines(material), width - textLeft - margin, height - margin * 2)
                .setPosition(textLeft, margin);
    }

    @Override
    public Identifier getIdentifier(ViewerMaterial material) {
        return Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "material/" + material.key());
    }

    private static List<FormattedText> lines(ViewerMaterial material) {
        MaterialData data = material.data();
        List<FormattedText> lines = new ArrayList<>();

        lines.add(name(material).copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal(material.category()).withStyle(ChatFormatting.DARK_GRAY));

        // Only what the material actually sets. Most carry a handful of these, and a page of zeroes
        // reads as noise rather than as information.
        stat(lines, "primary", data.primary);
        stat(lines, "secondary", data.secondary);
        stat(lines, "tertiary", data.tertiary);
        stat(lines, "durability", data.durability);
        stat(lines, "tool_level", data.toolLevel);
        stat(lines, "tool_efficiency", data.toolEfficiency);
        stat(lines, "magic_capacity", data.magicCapacity);
        integrity(lines, data);

        return lines;
    }

    /**
     * A material's name is a translation of its key where one exists. Some materials are keyed after
     * an item that has no material translation of its own, so the key itself is the fallback.
     */
    private static Component name(ViewerMaterial material) {
        String key = "tetra.material." + material.key();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(material.key());
    }

    /**
     * Integrity is two numbers the holosphere shows as gain and cost together, so this follows it
     * rather than splitting them into two lines that would read as unrelated.
     */
    private static void integrity(List<FormattedText> lines, MaterialData data) {
        if (data.integrityGain == 0 && data.integrityCost == 0) {
            return;
        }

        lines.add(label("integrity")
                .append(Component.literal(format(data.integrityGain)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(format(data.integrityCost)).withStyle(ChatFormatting.RED)));
    }

    private static void stat(List<FormattedText> lines, String key, @Nullable Number value) {
        if (value == null || value.floatValue() == 0) {
            return;
        }

        lines.add(label(key).append(Component.literal(format(value.floatValue())).withStyle(ChatFormatting.YELLOW)));
    }

    private static net.minecraft.network.chat.MutableComponent label(String key) {
        return Component.translatable(statPrefix + key).append(": ").withStyle(ChatFormatting.GRAY);
    }

    private static String format(float value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
    }
}
