package se.mickelus.tetra.compat.viewer;

import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.MaterialData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Reads Tetra's data into shapes a recipe viewer can browse.
 *
 * Tetra's crafting is not a set of recipes. A schematic's outcome is a function of the target item,
 * the slot and the materials, so exporting it as recipes would be a product of 343 schematics, 78
 * modules and 70 materials. What does export cleanly is the material list, which is also the
 * question players actually ask: what can I use this for.
 *
 * Nothing here imports a viewer's api. JEI reads it in se.mickelus.tetra.compat.jei and EMI reads
 * the same thing in se.mickelus.tetra.compat.emi, both drawing the text in MaterialSummary. EMI
 * support builds against EMI Refreshed, because upstream EMI stops at 1.21.1.
 */
public final class TetraViewerContent {
    private TetraViewerContent() {
    }

    /**
     * Every material a player can actually obtain and use, sorted by category and then key so the
     * list reads in the same order the holosphere shows.
     */
    public static List<ViewerMaterial> materials() {
        return DataManager.instance.materialData.getData().values().stream()
                .filter(Objects::nonNull)
                .filter(material -> !material.hidden)
                .filter(material -> material.material != null && material.material.isValid())
                .map(TetraViewerContent::toViewerMaterial)
                .filter(material -> !material.items().isEmpty())
                .sorted(Comparator.comparing(ViewerMaterial::category).thenComparing(ViewerMaterial::key))
                .toList();
    }

    private static ViewerMaterial toViewerMaterial(MaterialData material) {
        return new ViewerMaterial(
                material.key,
                material.category,
                // Resolved rather than left as a tag, because a viewer shows items and a tag that
                // resolves to nothing should drop the material rather than render an empty slot.
                Arrays.asList(material.material.getApplicableItemStacks()),
                material);
    }
}
