package se.mickelus.tetra.module.model;

import com.mojang.math.Transformation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import se.mickelus.mutil.gui.SimpleColor;
import se.mickelus.tetra.items.modular.ItemColors;
import se.mickelus.tetra.module.Priority;
import se.mickelus.tetra.module.data.MaterialData;

import java.util.Arrays;
import java.util.List;

public class GridTextureModelData extends AbstractTextureModelData {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("tetra", "grid_texture");

    public GridTextureModelData() {
        super();
    }

    public GridTextureModelData(Identifier location) {
        this(TYPE, location, null, null, 0, null, null, null, false, null);
    }

    public GridTextureModelData(Identifier type, Identifier location, Identifier renderType, Transformation transform,
            Integer emission, SimpleColor tint, SimpleColor overlayTint, Priority renderLayer, Boolean invertPerspectives,
            ItemDisplayContext[] contexts) {
        super();
        this.type = type;
        this.location = location;
        this.renderType = renderType;
        this.transform = transform;
        if (emission != null) {
            this.emission = Mth.clamp(0, emission, 15);
        }
        if (tint != null) {
            this.tint = tint;
        }
        if (overlayTint != null) {
            this.overlayTint = overlayTint;
        }
        if (renderLayer != null) {
            this.renderLayer = renderLayer;
        }
        if (invertPerspectives != null) {
            this.invertPerspectives = invertPerspectives;
        }
        this.contexts = contexts;
    }

    /**
     * The texture class a module offers for materials that carry a colour palette.
     *
     * Artwork under this name is drawn once in greyscale. The atlas builds one recoloured sprite per
     * material from it, named for the material, which is what the palette suffix below selects. A
     * module that offers it lets any palette carrying material use it without artwork of its own.
     */
    public static final String paletteTexture = "greyscale";

    public GridTextureModelData forMaterial(List<String> availableTextures, MaterialData material) {
        if (material.palette != null && availableTextures.contains(paletteTexture)) {
            GridTextureModelData copy = copy();
            copy.location = appendString(location, paletteTexture);
            copy.paletteSuffix = material.key;
            // The palette has already coloured every pixel, so a tint on top would only muddy it.
            copy.tint = new SimpleColor(0xffffffff);
            copy.overlayTint = new SimpleColor(material.tints.texture);
            return copy;
        }

        if (Arrays.stream(material.textureOverrides).anyMatch(override -> location.getPath().equals(override))) {
            GridTextureModelData copy = copy();
            copy.location = appendString(location, material.textures[0]);
            copy.tint = material.tintOverrides ? new SimpleColor(material.tints.texture) : new SimpleColor(0xffffffff);
            copy.overlayTint = new SimpleColor(material.tints.texture);
            return copy;
        }

        Identifier updatedLocation = Arrays.stream(material.textures)
                .filter(availableTextures::contains)
                .findFirst()
                .map(texture -> appendString(location, texture))
                .orElseGet(() -> appendString(location, availableTextures.get(0)));
        GridTextureModelData copy = copy();
        copy.location = updatedLocation;
        copy.tint = new SimpleColor(material.tints.texture);
        copy.overlayTint = new SimpleColor(material.tints.texture);
        return copy;
    }

    protected static Identifier appendString(Identifier resourceLocation, String string) {
        return Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + string);
    }

    /**
     * The material whose palette recoloured this layer, or null when it is ordinary artwork.
     *
     * It is applied last, after the slot suffix, because the atlas appends its permutation name to
     * the end of the texture it was given.
     */
    public Identifier getPaletteLocation() {
        return paletteSuffix != null
                ? appendString(location, "_" + paletteSuffix)
                : location;
    }

    public GridTextureModelData withSlotSuffix(String suffix) {
        GridTextureModelData copy = copy();
        copy.location = Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath() + suffix);
        return copy;
    }

    public GridTextureModelData inheritTint(SimpleColor parentTint) {
        if (ItemColors.inherit == tint.getRaw()) {
            GridTextureModelData copy = copy();
            copy.tint = parentTint;
            return copy;
        }
        return this;
    }

    public GridTextureModelData copy() {
        GridTextureModelData copy = new GridTextureModelData(
                type,
                location,
                renderType,
                transform,
                emission,
                tint,
                overlayTint,
                renderLayer,
                invertPerspectives,
                contexts);
        // The palette outlives every later copy, including the slot suffix, because it names the
        // sprite the atlas built rather than anything about this layer.
        copy.paletteSuffix = paletteSuffix;
        return copy;
    }
}
