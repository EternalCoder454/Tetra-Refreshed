package se.mickelus.tetra.compat.viewer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import se.mickelus.tetra.module.data.MaterialData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What a material page says, as lines of text.
 *
 * Both viewer plugins render this, so a material reads the same in either one and there is a single
 * place to change what a material page tells you.
 *
 * The stat labels are the holosphere's own. Using them means the page is worded the way the mod
 * already words these numbers, rather than inventing a second vocabulary for them.
 */
public final class MaterialSummary {
    private static final String statPrefix = "tetra.holo.craft.materials.stat.";

    private MaterialSummary() {
    }

    public static List<Component> lines(ViewerMaterial material) {
        MaterialData data = material.data();
        List<Component> lines = new ArrayList<>();

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
    public static Component name(ViewerMaterial material) {
        String key = "tetra.material." + material.key();
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(material.key());
    }

    /**
     * Integrity is two numbers the holosphere shows as gain and cost together, so this follows it
     * rather than splitting them into two lines that would read as unrelated.
     */
    private static void integrity(List<Component> lines, MaterialData data) {
        if (data.integrityGain == 0 && data.integrityCost == 0) {
            return;
        }

        lines.add(label("integrity")
                .append(Component.literal(format(data.integrityGain)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(format(data.integrityCost)).withStyle(ChatFormatting.RED)));
    }

    private static void stat(List<Component> lines, String key, @Nullable Number value) {
        if (value == null || value.floatValue() == 0) {
            return;
        }

        lines.add(label(key).append(Component.literal(format(value.floatValue())).withStyle(ChatFormatting.YELLOW)));
    }

    private static MutableComponent label(String key) {
        return Component.translatable(statPrefix + key).append(": ").withStyle(ChatFormatting.GRAY);
    }

    private static String format(float value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
    }
}
