package se.mickelus.tetra;

import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TetraItemAbilities {
    // todo 1.12: replace cut with vanilla SWORD_DIG
    public static final ItemAbility cut = ItemAbility.get("cut");

    // NeoForge declared these until vanilla moved block breaking onto the tool data component
    // and block tags. Tetra never used them to ask whether a block can be broken, only as
    // identity keys for which tool a stat, effect or sorter is about, which an ItemAbility
    // still is. The names are the ones NeoForge interned, so anything else keyed on the old
    // names still lines up.
    public static final ItemAbility AXE_DIG = ItemAbility.get("axe_dig");
    public static final ItemAbility PICKAXE_DIG = ItemAbility.get("pickaxe_dig");
    public static final ItemAbility SHOVEL_DIG = ItemAbility.get("shovel_dig");
    public static final ItemAbility HOE_DIG = ItemAbility.get("hoe_dig");
    public static final ItemAbility SWORD_DIG = ItemAbility.get("sword_dig");
    public static final ItemAbility hammer = ItemAbility.get("hammer_dig");
    public static final ItemAbility pry = ItemAbility.get("pry");
    public static final ItemAbility dowse = ItemAbility.get("dowse");
}
