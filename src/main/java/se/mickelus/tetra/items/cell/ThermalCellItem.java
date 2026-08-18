package se.mickelus.tetra.items.cell;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.function.Supplier;
import se.mickelus.tetra.items.TetraItem;

import javax.annotation.ParametersAreNonnullByDefault;

import static se.mickelus.tetra.blocks.forged.ForgedBlockCommon.locationTooltip;

@ParametersAreNonnullByDefault
public class ThermalCellItem extends TetraItem {
    public static final int maxCharge = 128;
    public static final String identifier = "thermal_cell";
    public static Supplier<ThermalCellItem> instance;

    public ThermalCellItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(maxCharge)
        );
    }

    public static int getCharge(ItemStack itemStack) {
        return itemStack.getMaxDamage() - itemStack.getDamageValue();
    }

    public static int drainCharge(ItemStack itemStack, int amount) {
        if (itemStack.getDamageValue() + amount < itemStack.getMaxDamage()) {
            itemStack.setDamageValue(itemStack.getDamageValue() + amount);
            return amount;
        }

        int actualAmount = itemStack.getMaxDamage() - itemStack.getDamageValue();
        itemStack.setDamageValue(itemStack.getMaxDamage());
        return actualAmount;
    }

    public static int recharge(ItemStack itemStack, int amount) {
        if (itemStack.getDamageValue() - amount >= 0) {
            itemStack.setDamageValue(itemStack.getDamageValue() - amount);
            return 0;
        }

        int overfill = amount - itemStack.getDamageValue();
        itemStack.setDamageValue(0);
        return overfill;
    }


    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag advanced) {
        int charge = getCharge(stack);

        MutableComponent chargeLine;

        if (charge == maxCharge) {
            chargeLine = Component.translatable("item.tetra.thermal_cell.charge", Component.translatable("item.tetra.thermal_cell.charge_full"));
        } else if (charge > maxCharge * 0.4) {
            chargeLine = Component.translatable("item.tetra.thermal_cell.charge", Component.translatable("item.tetra.thermal_cell.charge_good"));
        } else if (charge > 0) {
            chargeLine = Component.translatable("item.tetra.thermal_cell.charge", Component.translatable("item.tetra.thermal_cell.charge_low"));
        } else {
            chargeLine = Component.translatable("item.tetra.thermal_cell.charge", Component.translatable("item.tetra.thermal_cell.charge_empty"));
        }

        tooltip.accept(chargeLine);
        tooltip.accept(Component.literal(" "));
        tooltip.accept(locationTooltip);
    }

    // todo: change these for metered upgrade
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    /*
    public double getDurabilityForDisplay(ItemStack itemStack) {
        return super.getDurabilityForDisplay(itemStack);
    }


    public int getRGBDurabilityForDisplay(ItemStack itemStack) {
        return super.getRGBDurabilityForDisplay(itemStack);
    }
     */
}
