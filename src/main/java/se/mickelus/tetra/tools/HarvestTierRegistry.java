package se.mickelus.tetra.tools;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class HarvestTierRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final List<ToolMaterial> orderedTiers = new ArrayList<>();
    private static final Map<ToolMaterial, TierEntry> entriesByTier = new IdentityHashMap<>();
    private static final Map<Identifier, ToolMaterial> tiersByName = new LinkedHashMap<>();
    private static final Set<Identifier> unknownTierWarnings = ConcurrentHashMap.newKeySet();
    private static int nextOrder;

    static {
        registerVanilla(ToolMaterial.WOOD, "wood", List.of(), List.of());
        registerVanilla(ToolMaterial.GOLD, "gold", List.of(ToolMaterial.WOOD), List.of());
        registerVanilla(ToolMaterial.STONE, "stone", List.of(ToolMaterial.GOLD), List.of());
        registerVanilla(ToolMaterial.IRON, "iron", List.of(ToolMaterial.STONE), List.of());
        registerVanilla(ToolMaterial.DIAMOND, "diamond", List.of(ToolMaterial.IRON), List.of());
        registerVanilla(ToolMaterial.NETHERITE, "netherite", List.of(ToolMaterial.DIAMOND), List.of());
    }

    private HarvestTierRegistry() {}

    private static void registerVanilla(ToolMaterial tier, String path, List<ToolMaterial> after, List<ToolMaterial> before) {
        registerInternal(tier, Identifier.withDefaultNamespace(path), after, before);
    }

    public static ToolMaterial register(ToolMaterial tier, Identifier name, List<ToolMaterial> after, List<ToolMaterial> before) {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(before, "before");

        if (entriesByTier.containsKey(tier)) {
            Identifier existingName = nameOf(tier);
            if (!name.equals(existingName)) {
                throw new IllegalStateException("ToolMaterial " + tier + " is already registered as " + existingName + ", not " + name);
            }
            return tier;
        }

        registerInternal(tier, name, after, before);
        return tier;
    }

    private static void registerInternal(ToolMaterial tier, Identifier name, List<ToolMaterial> after, List<ToolMaterial> before) {
        ToolMaterial namedTier = tiersByName.get(name);
        if (namedTier != null && namedTier != tier) {
            throw new IllegalStateException("ToolMaterial name already registered: " + name);
        }

        validateDependencies(name, after, "after");
        validateDependencies(name, before, "before");

        entriesByTier.put(tier, new TierEntry(name, List.copyOf(after), List.copyOf(before), nextOrder++));
        tiersByName.put(name, tier);
        rebuildOrderedTiers();
    }

    private static void validateDependencies(Identifier name, List<ToolMaterial> dependencies, String direction) {
        for (ToolMaterial dependency : dependencies) {
            if (!entriesByTier.containsKey(dependency)) {
                throw new IllegalStateException("Unknown " + direction + " dependency for tier " + name + ": " + dependency);
            }
        }
    }

    private static void rebuildOrderedTiers() {
        Map<ToolMaterial, Set<ToolMaterial>> edges = new IdentityHashMap<>();
        Map<ToolMaterial, Integer> indegree = new IdentityHashMap<>();

        for (ToolMaterial tier : entriesByTier.keySet()) {
            edges.put(tier, new LinkedHashSet<>());
            indegree.put(tier, 0);
        }

        for (Map.Entry<ToolMaterial, TierEntry> entry : entriesByTier.entrySet()) {
            ToolMaterial tier = entry.getKey();
            TierEntry data = entry.getValue();

            for (ToolMaterial dependency : data.after()) {
                addEdge(edges, indegree, dependency, tier);
            }
            for (ToolMaterial dependency : data.before()) {
                addEdge(edges, indegree, tier, dependency);
            }
        }

        PriorityQueue<ToolMaterial> ready = new PriorityQueue<>(Comparator.comparingInt(tier -> entriesByTier.get(tier).order()));
        for (Map.Entry<ToolMaterial, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<ToolMaterial> resolved = new ArrayList<>(entriesByTier.size());
        while (!ready.isEmpty()) {
            ToolMaterial tier = ready.remove();
            resolved.add(tier);

            for (ToolMaterial dependent : edges.get(tier)) {
                int remaining = indegree.computeIfPresent(dependent, (ignored, value) -> value - 1);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (resolved.size() != entriesByTier.size()) {
            String cycle = entriesByTier.entrySet().stream()
                    .filter(entry -> indegree.get(entry.getKey()) > 0)
                    .sorted(Comparator.comparingInt(entry -> entry.getValue().order()))
                    .map(entry -> entry.getValue().name().toString())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Cyclic tier dependencies detected: " + cycle);
        }

        orderedTiers.clear();
        orderedTiers.addAll(resolved);
    }

    private static void addEdge(Map<ToolMaterial, Set<ToolMaterial>> edges, Map<ToolMaterial, Integer> indegree, ToolMaterial source, ToolMaterial target) {
        if (edges.get(source).add(target)) {
            indegree.computeIfPresent(target, (ignored, value) -> value + 1);
        }
    }

    @Nullable
    public static ToolMaterial byName(@Nullable Identifier name) {
        if (name == null) {
            return null;
        }

        ToolMaterial tier = tiersByName.get(name);
        if (tier == null && unknownTierWarnings.add(name)) {
            logger.warn("Unknown harvest tier '{}', falling back to level 0", name);
        }
        return tier;
    }

    public static List<ToolMaterial> ordered() {
        return List.copyOf(orderedTiers);
    }

    @Nullable
    public static Identifier nameOf(ToolMaterial tier) {
        TierEntry entry = entriesByTier.get(tier);
        return entry != null ? entry.name() : null;
    }

    public static boolean isCorrectTierForDrops(ToolMaterial tier, BlockState state) {
        return !state.is(tier.incorrectBlocksForDrops());
    }

    private record TierEntry(Identifier name, List<ToolMaterial> after, List<ToolMaterial> before, int order) {}
}
