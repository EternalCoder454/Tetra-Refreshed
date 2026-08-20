package se.mickelus.tetra.effect;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import se.mickelus.tetra.effect.howling.HowlingEffect;
import se.mickelus.tetra.effect.lunge.LungeEffect;
import se.mickelus.tetra.items.modular.ItemModularHandheld;

import static se.mickelus.tetra.effect.EffectHelper.getEffectLevel;

/**
 * The input handlers that need the client's own player and key bindings.
 *
 * These were on ItemEffectHandler behind an @OnlyIn(Dist.CLIENT). NeoForge does not strip @OnlyIn
 * from mod classes, so ItemEffectHandler carried references to Minecraft and KeyMapping in its own
 * bytecode and could not load on a dedicated server, taking every server side handler on that class
 * down with it. Keeping the client input here is what @OnlyIn was being asked to do and cannot.
 */
public class ClientInputHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack itemStack = mc.player.getMainHandItem();
        if (event.isAttack()
                && !event.isCanceled()
                && itemStack.getItem() instanceof ItemModularHandheld
                && mc.hitResult != null
                && HitResult.Type.MISS.equals(mc.hitResult.getType())) {
            if (getEffectLevel(itemStack, ItemEffect.truesweep) > 0) {
                SweepingEffect.triggerTruesweep();
            }
            if (getEffectLevel(itemStack, ItemEffect.howling) > 0) {
                HowlingEffect.sendPacket();
            }
        }

        if (event.isUseItem()) {
            LungeEffect.onRightClick(mc.player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKeyInput(InputEvent.Key event) {
        KeyMapping jumpKey = Minecraft.getInstance().options.keyJump;
        if (jumpKey.matches(event.getKeyEvent()) && jumpKey.isDown()) {
            LungeEffect.onJump(Minecraft.getInstance().player);
        }
    }
}
