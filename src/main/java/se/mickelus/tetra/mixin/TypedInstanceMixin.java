package se.mickelus.tetra.mixin;

import com.google.common.collect.Streams;
import net.minecraft.core.TypedInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.items.modular.IModularItem;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Lets a modular item answer for the tags its modules give it.
 *
 * These two injections used to sit on ItemStack.is(TagKey) and ItemStack.getTags(). Both moved onto
 * the TypedInstance interface as default methods, is(TagKey) and tags(), which ItemStack inherits,
 * so the injection target moved with them. Everything that carries a tagged type goes through here,
 * hence the ItemStack check before doing any work.
 */
@Mixin(TypedInstance.class)
public interface TypedInstanceMixin {
    @Inject(at = @At("RETURN"), method = "is(Lnet/minecraft/tags/TagKey;)Z", cancellable = true)
    private void isWithModularTags(TagKey<?> tag, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() || !((Object) this instanceof ItemStack itemStack)
                || !(itemStack.getItem() instanceof IModularItem item)) {
            return;
        }

        Set<TagKey<Item>> tags = item.getPropertiesCached(itemStack).tags;
        if (tags != null) {
            callback.setReturnValue(tags.contains(tag));
        }
    }

    @Inject(at = @At("RETURN"), method = "tags()Ljava/util/stream/Stream;", cancellable = true)
    private void tagsWithModularTags(CallbackInfoReturnable<Stream<TagKey<?>>> callback) {
        if (!((Object) this instanceof ItemStack itemStack) || !(itemStack.getItem() instanceof IModularItem item)) {
            return;
        }

        Set<TagKey<Item>> tags = item.getPropertiesCached(itemStack).tags;
        if (tags != null) {
            callback.setReturnValue(Streams.concat(callback.getReturnValue(), tags.stream()));
        }
    }
}
