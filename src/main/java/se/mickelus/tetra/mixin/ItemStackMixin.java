package se.mickelus.tetra.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.aspect.TetraEnchantmentHelper;
import se.mickelus.tetra.items.modular.IModularItem;

import javax.annotation.ParametersAreNonnullByDefault;

// Cross-version compat: matches upstream 1.20's ItemStackMixin.
// 1.21 changed enchant() to take Holder<Enchantment>; the rest of the injections are unchanged.
// The tag lookups moved off ItemStack onto the TypedInstance interface, so they live in
// TypedInstanceMixin now.
@ParametersAreNonnullByDefault
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(at = @At("RETURN"),
            method = "enchant(Lnet/minecraft/core/Holder;I)V")
    private void addEnchantment(Holder<Enchantment> enchantment, int level, CallbackInfo callback) {
        if (getItem() instanceof IModularItem item) {
            ItemStack itemStack = getInstance();
            TetraEnchantmentHelper.mapEnchantments(itemStack);
            item.assemble(itemStack, null, 0);
        }
    }

    @Shadow
    public Item getItem() {
        throw new IllegalStateException("Mixin failed to shadow getItem()");
    }

    private ItemStack getInstance() {
        return ((ItemStack) (Object) this);
    }
}
