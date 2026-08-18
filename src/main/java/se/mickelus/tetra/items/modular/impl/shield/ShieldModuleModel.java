package se.mickelus.tetra.items.modular.impl.shield;

import net.minecraft.resources.Identifier;
import se.mickelus.mutil.gui.SimpleColor;
import se.mickelus.tetra.items.modular.ItemColors;
import se.mickelus.tetra.module.Priority;
import se.mickelus.tetra.module.data.MaterialData;
import se.mickelus.tetra.module.model.IModuleModel;

import java.util.Arrays;
import java.util.List;

public class ShieldModuleModel implements IModuleModel {
    protected Identifier type;
    protected Identifier model;
    protected Identifier texture;
    protected SimpleColor tint;
    protected SimpleColor overlayTint;
    protected Priority renderLayer = Priority.BASE;

    public ShieldModuleModel(Identifier type, Identifier model, Identifier texture, SimpleColor tint, SimpleColor overlayTint,
            Priority renderLayer) {
        this.type = type;
        this.model = model;
        this.texture = texture;
        this.tint = tint;
        this.overlayTint = overlayTint;
        this.renderLayer = renderLayer;
    }

    @Override
    public Identifier getType() {
        return type;
    }

    @Override
    public Priority getRenderLayer() {
        return renderLayer;
    }

    public Identifier getModel() {
        return model;
    }

    public Identifier getTexture() {
        return texture;
    }

    public SimpleColor getTint() {
        return tint;
    }

    public SimpleColor getOverlayTint() {
        return overlayTint;
    }

    public ShieldModuleModel forMaterial(List<String> availableTextures, MaterialData material) {
        if (Arrays.stream(material.textureOverrides).anyMatch(override -> texture.getPath().equals(override))) {
            ShieldModuleModel copy = copy();
            copy.texture = appendString(texture, material.textures[0]);
            copy.tint = material.tintOverrides ? new SimpleColor(material.tints.texture) : new SimpleColor(0xffffffff);
            copy.overlayTint = new SimpleColor(material.tints.texture);
            return copy;
        }

        Identifier updatedLocation = Arrays.stream(material.textures)
                .filter(availableTextures::contains)
                .findFirst()
                .map(texture -> appendString(this.texture, texture))
                .orElseGet(() -> appendString(this.texture, availableTextures.get(0)));
        ShieldModuleModel copy = copy();
        copy.texture = updatedLocation;
        copy.tint = new SimpleColor(material.tints.texture);
        copy.overlayTint = new SimpleColor(material.tints.texture);
        return copy;
    }

    protected static Identifier appendString(Identifier resourceLocation, String string) {
        return Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + string);
    }

    public ShieldModuleModel withSlotSuffix(String suffix) {
        ShieldModuleModel copy = copy();
        copy.texture = Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + suffix);
        return copy;
    }

    public ShieldModuleModel inheritTint(SimpleColor parentTint) {
        if (ItemColors.inherit == tint.getRaw()) {
            ShieldModuleModel copy = copy();
            copy.tint = parentTint;
            return copy;
        }
        return this;
    }

    protected ShieldModuleModel copy() {
        return new ShieldModuleModel(type, model, texture, tint, overlayTint, renderLayer);
    }
}
