package se.mickelus.tetra.module;

import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.data.DataManager;

import javax.annotation.Nullable;
import se.mickelus.tetra.module.data.MaterialVariantData;
import se.mickelus.tetra.module.data.ModuleData;
import se.mickelus.tetra.module.data.VariantData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class ModuleRegistry {
    private static final Logger logger = LogManager.getLogger();

    public static ModuleRegistry instance;

    private final Map<Identifier, BiFunction<Identifier, ModuleData, ItemModule>> moduleConstructors;
    private Map<Identifier, ItemModule> moduleMap;

    // Modules are looked up by their bare key far more than by identifier, once per slot on every
    // read of an item's modules, and that runs on tooltips, attributes, tool data and rendering.
    // Going through an identifier meant allocating and validating one per lookup, so the same
    // modules are held here under their path as well.
    private Map<String, ItemModule> moduleKeyMap;

    public ModuleRegistry() {
        instance = this;

        moduleConstructors = new HashMap<>();
        moduleMap = Collections.emptyMap();
        moduleKeyMap = Collections.emptyMap();

        DataManager.instance.moduleData.onReload(() -> setupModules(DataManager.instance.moduleData.getData()));
    }

    private void setupModules(Map<Identifier, ModuleData> data) {
        moduleMap = data.entrySet().stream()
                .filter(entry -> validateModuleData(entry.getKey(), entry.getValue()))
                .flatMap(entry -> expandEntry(entry).stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> setupModule(entry.getKey(), entry.getValue())
                ));

        // Only the tetra namespaced ones, keyed by path, so that a bare key resolves to exactly
        // what building a tetra identifier from it would have resolved to.
        moduleKeyMap = moduleMap.entrySet().stream()
                .filter(entry -> TetraMod.MOD_ID.equals(entry.getKey().getNamespace()))
                .collect(Collectors.toMap(entry -> entry.getKey().getPath(), Map.Entry::getValue));
    }

    private boolean validateModuleData(Identifier identifier, ModuleData data) {
        if (data == null) {
            logger.warn("Failed to create module from module data '{}': Data is null (probably due to it failing to parse)",
                    identifier);
            return false;
        }

        if (!moduleConstructors.containsKey(data.type)) {
            logger.warn("Failed to create module from module data '{}': Unknown type '{}'", identifier, data.type);
            return false;
        }

        if (data.slots == null || data.slots.length < 1) {
            logger.warn("Failed to create module from module data '{}': Slots field is empty",
                    identifier);
            return false;
        }

        return true;
    }

    // todo: hacky stuff to get multislot modules to work, there has to be another way
    private Collection<Pair<Identifier, ModuleData>> expandEntry(Map.Entry<Identifier, ModuleData> entry) {
        ModuleData moduleData = entry.getValue();
        if (moduleData.slotSuffixes.length > 0) {
            ArrayList<Pair<Identifier, ModuleData>> result = new ArrayList<>(moduleData.slots.length);
            for (int i = 0; i < moduleData.slots.length; i++) {
                ModuleData dataCopy = moduleData.shallowCopy();
                dataCopy.slots = new String[] {moduleData.slots[i]};
                dataCopy.slotSuffixes = new String[] {moduleData.slotSuffixes[i]};

                Identifier suffixedIdentifier = Identifier.fromNamespaceAndPath(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath() + moduleData.slotSuffixes[i]);

                result.add(new ImmutablePair<>(suffixedIdentifier, dataCopy));
            }

            return result;
        }
        return Collections.singletonList(new ImmutablePair<>(entry.getKey(), entry.getValue()));
    }

    /**
     * Expands all material based variants for this module data.
     *
     * @param moduleData
     */
    private void expandMaterialVariants(ModuleData moduleData) {
        moduleData.variants = Arrays.stream(moduleData.variants)
                .flatMap(variant ->
                        variant instanceof MaterialVariantData
                                ? expandMaterialVariant((MaterialVariantData) variant)
                                : Stream.of(variant))
                .toArray(VariantData[]::new);
    }

    private Stream<VariantData> expandMaterialVariant(MaterialVariantData source) {
        return Arrays.stream(source.materials)
                .map(rl -> rl.getPath().endsWith("/")
                        ? DataManager.instance.materialData.getDataIn(rl)
                        : Optional.ofNullable(DataManager.instance.materialData.getData(rl)).map(Collections::singletonList).orElseGet(Collections::emptyList))
                .flatMap(Collection::stream)
                .map(source::combine);
    }

    private void handleVariantDuplicates(ModuleData data) {
        data.variants = Arrays.stream(data.variants)
                .collect(Collectors.toMap(variant -> variant.key, Function.identity(), VariantData::merge))
                .values()
                .toArray(new VariantData[0]);
    }

    private ItemModule setupModule(Identifier identifier, ModuleData data) {
        expandMaterialVariants(data);
        handleVariantDuplicates(data);

        return moduleConstructors.get(data.type).apply(identifier, data);
    }

    public void registerModuleType(Identifier identifier, BiFunction<Identifier, ModuleData, ItemModule> constructor) {
        moduleConstructors.put(identifier, constructor);
    }


    public ItemModule getModule(Identifier identifier) {
        return moduleMap.get(identifier);
    }

    /**
     * A module by its bare key, which is the tetra namespaced identifier of the same path. Same
     * answer as building that identifier and asking for it, without building it.
     */
    @Nullable
    public ItemModule getModule(String key) {
        return moduleKeyMap.get(key);
    }

    public Collection<ItemModule> getAllModules() {
        return moduleMap.values();
    }
}
