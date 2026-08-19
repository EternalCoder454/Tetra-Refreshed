package se.mickelus.tetra.data.deserializer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import se.mickelus.tetra.util.RegistryHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// Cross-version compat: this Gson deserializer mirrors upstream 1.20. Do not migrate to Codec /
// MapCodec — rewriting forks the codebase from upstream Tetra.
@ParametersAreNonnullByDefault
public class AttributesDeserializer implements JsonDeserializer<Multimap<Attribute, AttributeModifier>> {
    private static final Logger logger = LogManager.getLogger();

    public static final TypeToken<Multimap<Attribute, AttributeModifier>> typeToken = new TypeToken<Multimap<Attribute, AttributeModifier>>() {
    };
    // legacyAttributeIds: Forge-namespaced 1.20 attribute IDs that we translate into 1.21 vanilla IDs
    // at JSON parse time. Keep the map in sync with upstream 1.20 — JSON authored against 1.20 still
    // uses these forge: keys, and rewriting to vanilla namespaces would diverge from upstream.
    private static final Map<String, Identifier> legacyAttributeIds = Map.ofEntries(
            // 26.1 dropped the player prefix these carried in 1.21.1. Mapping to the old names
            // parsed fine and then resolved to nothing, so every reach modifier in the mod was
            // being discarded. The warning below is what surfaced it.
            Map.entry("forge:reach_distance", Identifier.withDefaultNamespace("block_interaction_range")),
            Map.entry("forge:block_reach", Identifier.withDefaultNamespace("block_interaction_range")),
            Map.entry("forge:attack_range", Identifier.withDefaultNamespace("entity_interaction_range")),
            Map.entry("forge:entity_reach", Identifier.withDefaultNamespace("entity_interaction_range")),
            // The generic prefix went in 1.21. These five parse as a valid identifier and simply
            // resolve to nothing, and getAttribute drops a modifier it cannot resolve without
            // saying so, which left every module contributing no damage, speed, armor or toughness.
            Map.entry("generic.attack_damage", Identifier.withDefaultNamespace("attack_damage")),
            Map.entry("generic.attack_speed", Identifier.withDefaultNamespace("attack_speed")),
            Map.entry("generic.armor", Identifier.withDefaultNamespace("armor")),
            Map.entry("generic.armor_toughness", Identifier.withDefaultNamespace("armor_toughness")),
            Map.entry("generic.movement_speed", Identifier.withDefaultNamespace("movement_speed")),
            // Two of Tetra's own. draw_speed is written with a dot in one addon file, which parses
            // as a path in the minecraft namespace and resolves to nothing.
            Map.entry("tetra.draw_speed", Identifier.fromNamespaceAndPath("tetra", "draw_speed")),
            // draw_damage is a name Tetra never registered, and it sits in this mod's own quality
            // and ravenous improvements as the ranged third of a damage triple, beside
            // attack_damage and ability_damage. draw_strength is the one ModularBowItem reads for
            // bow damage, so that is where it points. Until now neither improvement raised a bow.
            Map.entry("tetra:draw_damage", Identifier.fromNamespaceAndPath("tetra", "draw_strength")));

    private static AttributeModifier.Operation getOperation(String key) {
        if (key.startsWith("**")) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        } else if (key.startsWith("*")) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        }

        return AttributeModifier.Operation.ADD_VALUE;
    }

    private static Attribute getAttribute(String key) {
        String resolvedKey = key.replace("*", "");
        Identifier rl = legacyAttributeIds.getOrDefault(resolvedKey, Identifier.parse(resolvedKey));

        return RegistryHelper.get(BuiltInRegistries.ATTRIBUTE, rl);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ArrayListMultimap<Attribute, AttributeModifier> result = ArrayListMultimap.create();

        jsonObject.entrySet().forEach(entry -> {
            Attribute attribute = getAttribute(entry.getKey());
            if (attribute == null) {
                // Saying so, because dropping these in silence is what hid the missing generic
                // prefix mapping and left every modular item without its module attributes.
                logger.warn("Unknown attribute '{}', its modifier is ignored", entry.getKey());
            } else {
                result.put(attribute, new AttributeModifier(Identifier.fromNamespaceAndPath("tetra", "module_data"), entry.getValue().getAsDouble(), getOperation(entry.getKey())));
            }
        });

        return result;
    }
}
