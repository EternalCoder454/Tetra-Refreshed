package se.mickelus.tetra.items.modular.impl.dynamic;

/**
 * A handheld modular item type, defined in data rather than in java.
 *
 * The store key is the file path under data/tetra/archetypes, so the definition carries no id of
 * its own. Everything a simple handheld type needs to declare lives here. What it cannot express is
 * custom stat combination, so a type that has to merge two heads without stacking their damage is
 * still a java class.
 *
 * @param honeable                whether the type gains hone progress at all
 * @param honeBase                uses required before the first hone
 * @param honeIntegrityMultiplier per point of integrity, added to the uses required
 * @param synergyPrefix           directory under data/tetra/synergies whose synergies apply
 * @param entityHitDamage         durability spent per entity hit, at least one
 * @param slots                   the module slots, their kind and their position in the workbench
 */
public record ArchetypeDefinition(boolean honeable, int honeBase, int honeIntegrityMultiplier, String synergyPrefix, int entityHitDamage,
        ArchetypeSlotDefinition[] slots) {

    public ArchetypeDefinition {
        // Gson leaves anything the file omits at its zero value, so the defaults are applied here
        // rather than being written out in every archetype.
        slots = slots != null ? slots : new ArchetypeSlotDefinition[0];
        entityHitDamage = Math.max(entityHitDamage, 1);
    }
}

