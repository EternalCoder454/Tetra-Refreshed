package se.mickelus.tetra.data.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * ArmorMaterial is a plain record now rather than a registry entry, so there is no registry to look
 * one up in by id. The vanilla materials are constants on ArmorMaterials, and this maps the ids that
 * used to name them onto those constants. A material outside this set can no longer be named in data.
 */
public class ArmorMaterialDeserializer implements JsonDeserializer<ArmorMaterial> {
    private static final Map<String, ArmorMaterial> vanillaMaterials = Map.of(
            "leather", ArmorMaterials.LEATHER,
            "copper", ArmorMaterials.COPPER,
            "chainmail", ArmorMaterials.CHAINMAIL,
            "iron", ArmorMaterials.IRON,
            "gold", ArmorMaterials.GOLD,
            "diamond", ArmorMaterials.DIAMOND,
            "turtle_scute", ArmorMaterials.TURTLE_SCUTE,
            "netherite", ArmorMaterials.NETHERITE,
            "armadillo_scute", ArmorMaterials.ARMADILLO_SCUTE);

    @Override
    public ArmorMaterial deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            ArmorMaterial material = vanillaMaterials.get(Identifier.parse(json.getAsString()).getPath());
            if (material != null) {
                return material;
            }
            throw new IllegalArgumentException("Unknown armor material: " + json.getAsString());
        } catch (Exception e) {
            throw new JsonParseException("Tried to parse faulty ArmorMaterial: " + json, e);
        }
    }
}
