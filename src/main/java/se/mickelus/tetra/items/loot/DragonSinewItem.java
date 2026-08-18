package se.mickelus.tetra.items.loot;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.items.TetraItem;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class DragonSinewItem extends TetraItem {
    public static final String identifier = "dragon_sinew";
    static final Component tooltip = Component.translatable("item.tetra." + identifier + ".description")
            .withStyle(ChatFormatting.GRAY);

    public DragonSinewItem() {
        super(new Properties());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(DragonSinewItem.tooltip);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(Level world, Entity entity, ItemStack itemstack) {
        entity.setNoGravity(true);

        return null;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.8f));
        if (entity.level().isClientSide() && entity.getAge() % 20 == 0) {
            entity.level().addParticle(ParticleTypes.DRAGON_BREATH, entity.getRandomX(.2d), entity.getRandomY() + 0.2, entity.getRandomZ(0.2),
                    entity.level().getRandom().nextFloat() * 0.02f - 0.01f, -0.01f - entity.level().getRandom().nextFloat() * 0.01f, entity.level().getRandom().nextFloat() * 0.02f - 0.01f);
        }
        return false;
    }
}
