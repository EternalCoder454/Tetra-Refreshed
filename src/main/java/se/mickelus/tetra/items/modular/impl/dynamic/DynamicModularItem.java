package se.mickelus.tetra.items.modular.impl.dynamic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.network.PacketHandler;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.gui.GuiModuleOffsets;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.data.SynergyData;
import se.mickelus.tetra.module.schematic.RepairSchematic;
import se.mickelus.tetra.util.ItemStackTagHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicModularItem extends ItemModularHandheld {

    public static final String identifier = "dynamic_handheld";
    public static final String typeKey = "archetype";

    // A fixed item type reads its synergies into a field once. Archetypes each name their own
    // directory, so the resolved arrays are cached per prefix and dropped when data reloads.
    private static final Map<String, SynergyData[]> synergyCache = new ConcurrentHashMap<>();

    public DynamicModularItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());

        // Every other modular item registers one of these in its constructor. Without it a dynamic
        // item has no repair schematic and cannot be repaired at all.
        SchematicRegistry.instance.registerSchematic(new RepairSchematic(this, identifier));
    }

    @Override
    public void commonInit(PacketHandler packetHandler) {
        DataManager.instance.synergyData.onReload(synergyCache::clear);
    }

    @Override
    public SynergyData[] getAllSynergyData(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::synergyPrefix)
                .filter(prefix -> !prefix.isEmpty())
                .map(prefix -> synergyCache.computeIfAbsent(prefix, DataManager.instance.synergyData::getOrdered))
                .orElseGet(() -> super.getAllSynergyData(itemStack));
    }

    @Override
    public int getEntityHitDamage(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::entityHitDamage)
                .orElseGet(() -> super.getEntityHitDamage(itemStack));
    }

    public static String getArchetypeKey(@Nullable CompoundTag tag) {
        return Optional.ofNullable(tag)
                .map(t -> t.getStringOr(typeKey, ""))
                .orElse(null);
    }

    public static String getArchetypeKey(ItemStack itemStack) {
        return getArchetypeKey(ItemStackTagHelper.readTag(itemStack));
    }

    public static void setArchetypeKey(ItemStack itemStack, String key) {
        ItemStackTagHelper.mutate(itemStack, tag -> tag.putString(typeKey, key));
    }

    protected Optional<ArchetypeDefinition> getDefinition(ItemStack itemStack) {
        return Optional.ofNullable(ItemStackTagHelper.readTag(itemStack))
                .map(DynamicModularItem::getArchetypeKey)
                .map(key -> Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, key))
                .map(rl -> DataManager.instance.archetypeData.getData(rl));
    }

    public String[] getMajorModuleKeys(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::slots)
                .map(slots -> Arrays.stream(slots)
                        .filter(ArchetypeSlotDefinition::major)
                        .map(ArchetypeSlotDefinition::key)
                        .toArray(String[]::new))
                .orElse(new String[0]);
    }

    public String[] getMinorModuleKeys(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::slots)
                .map(slots -> Arrays.stream(slots)
                        .filter(definition -> !definition.major())
                        .map(ArchetypeSlotDefinition::key)
                        .toArray(String[]::new))
                .orElse(new String[0]);
    }

    public String[] getRequiredModules(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::slots)
                .map(slots -> Arrays.stream(slots)
                        .filter(ArchetypeSlotDefinition::required)
                        .map(ArchetypeSlotDefinition::key)
                        .toArray(String[]::new))
                .orElse(new String[0]);
    }

    @Override
    public boolean canGainHoneProgress(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::honeable)
                .orElse(false);
    }

    @Override
    public int getHoneBase(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::honeBase)
                .orElse(0);
    }

    @Override
    public int getHoneIntegrityMultiplier(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::honeIntegrityMultiplier)
                .orElse(0);
    }

    @Override
    public GuiModuleOffsets getMajorGuiOffsets(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::slots)
                .map(slots -> Arrays.stream(slots)
                        .filter(ArchetypeSlotDefinition::major)
                        .toArray(ArchetypeSlotDefinition[]::new))
                .map(GuiModuleOffsets::new)
                .orElse(new GuiModuleOffsets());
    }

    @Override
    public GuiModuleOffsets getMinorGuiOffsets(ItemStack itemStack) {
        return getDefinition(itemStack)
                .map(ArchetypeDefinition::slots)
                .map(slots -> Arrays.stream(slots)
                        .filter(slot -> !slot.major())
                        .toArray(ArchetypeSlotDefinition[]::new))
                .map(GuiModuleOffsets::new)
                .orElse(new GuiModuleOffsets());
    }
}
