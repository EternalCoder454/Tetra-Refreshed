package se.mickelus.tetra.data.deserializer;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.dynamic.DynamicModularItem;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.ItemModuleMajor;
import se.mickelus.tetra.module.ItemUpgradeRegistry;
import se.mickelus.tetra.module.ReplacementDefinition;
import se.mickelus.tetra.util.RegistryHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.Map;
import se.mickelus.tetra.util.NonNullLazy;

@ParametersAreNonnullByDefault
public class ReplacementDeserializer implements JsonDeserializer<ReplacementDefinition> {
    private static final Logger logger = LogManager.getLogger();
    private static final RegistryAccess.Frozen builtinRegistryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();

    private static RegistryOps<JsonElement> createRegistryOps() {
        RegistryAccess registryAccess = ServerLifecycleHooks.getCurrentServer() != null
                ? ServerLifecycleHooks.getCurrentServer().registryAccess()
                : builtinRegistryAccess;
        return RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    }

    @Override
    public ReplacementDefinition deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {
        ReplacementDefinition replacement = new ReplacementDefinition();
        JsonObject jsonObject = element.getAsJsonObject();

        try {
            ItemPredicate predicate = ItemPredicate.CODEC.parse(createRegistryOps(), GsonHelper.getAsJsonObject(jsonObject, "predicate"))
                    .getOrThrow(JsonSyntaxException::new);
            replacement.predicate = predicate::test;
        } catch (JsonSyntaxException e) {
            throw new JsonSyntaxException("Failed to parse replacement data due to faulty predicate", e);
        }

        Identifier resourceLocation = Identifier.parse(GsonHelper.getAsString(jsonObject, "item"));
        Item item = RegistryHelper.get(BuiltInRegistries.ITEM, resourceLocation);
        if (item == null) {
            throw new JsonSyntaxException("Failed to parse replacement data, missing (or faulty) item in " + jsonObject.getAsString());
        }
        // Assembled on first use. Data is parsed while item components are unbound, because a
        // datapack reload rebinds them, so building a stack here throws "Components not bound yet".
        // The module lookups still happen at parse time so faulty data is still reported then.
        if (item instanceof IModularItem) {
            for (Map.Entry<String, JsonElement> moduleDefinition : GsonHelper.getAsJsonObject(jsonObject, "modules").entrySet()) {
                String moduleKey = moduleDefinition.getValue().getAsJsonArray().get(0).getAsString();
                if (ItemUpgradeRegistry.instance.getModule(moduleKey) == null) {
                    throw new JsonSyntaxException("Failed to parse replacement data due to missing module: " + moduleKey);
                }
            }
        }

        replacement.itemStack = NonNullLazy.of(() -> {
            ItemStack itemStack = new ItemStack(item);

            if (item instanceof IModularItem) {
                for (Map.Entry<String, JsonElement> moduleDefinition : GsonHelper.getAsJsonObject(jsonObject, "modules").entrySet()) {
                    String moduleVariant = moduleDefinition.getValue().getAsJsonArray().get(1).getAsString();
                    ItemModule module = ItemUpgradeRegistry.instance.getModule(
                            moduleDefinition.getValue().getAsJsonArray().get(0).getAsString());
                    if (module != null) {
                        module.addModule(itemStack, moduleVariant, null);
                    }
                }

                if (jsonObject.has("improvements")) {
                    for (Map.Entry<String, JsonElement> improvement : GsonHelper.getAsJsonObject(jsonObject, "improvements").entrySet()) {
                        String[] temp = improvement.getKey().split(":");
                        ItemModuleMajor.addImprovement(itemStack, temp[0], temp[1], improvement.getValue().getAsInt());
                    }
                }

                if (jsonObject.has("archetype")) {
                    DynamicModularItem.setArchetypeKey(itemStack, jsonObject.get("archetype").getAsString());
                }
            }

            return itemStack;
        });

        return replacement;
    }
}
