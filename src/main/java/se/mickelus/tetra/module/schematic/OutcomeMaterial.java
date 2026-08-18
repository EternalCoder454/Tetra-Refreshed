package se.mickelus.tetra.module.schematic;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import se.mickelus.tetra.data.deserializer.ItemPredicateDeserializer;
import se.mickelus.tetra.data.predicate.TetraItemPredicate;
import se.mickelus.tetra.util.RegistryHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static se.mickelus.tetra.util.ItemStackTagHelper.setTag;
import se.mickelus.tetra.util.NonNullLazy;
import java.util.List;

@ParametersAreNonnullByDefault
public class OutcomeMaterial {
    private static final JsonArray emptyArray = new JsonArray();

    public int count = 1;

    /**
      * The items this material accepts, and the stacks built from them.
      *
      * Data is parsed while item components are unbound, because a datapack reload rebinds them, so
      * building a stack in a deserializer throws "Components not bound yet". The parsed inputs are
      * held instead and the stacks are built the first time something reads them.
      */
    protected Collection<Item> items = Collections.emptyList();
    protected CompoundTag itemTag;
    private NonNullLazy<Collection<ItemStack>> itemStacks = NonNullLazy.of(this::buildItemStacks);
    protected TagKey<Item> tagLocation;

    private Collection<ItemStack> buildItemStacks() {
        return items.stream()
                .map(item -> new ItemStack(item, count))
                .peek(itemStack -> {
                    if (itemTag != null) {
                        setTag(itemStack, itemTag.copy());
                    }
                })
                .collect(Collectors.toList());
    }

    protected Collection<ItemStack> getItemStacks() {
        return itemStacks.get();
    }

    private TetraItemPredicate predicate;

    public OutcomeMaterial offsetCount(float multiplier, int offset) {
        OutcomeMaterial result = new OutcomeMaterial();
        result.count = Math.round(count * multiplier) + offset;

        result.items = items;
        result.itemTag = itemTag;

        result.tagLocation = tagLocation;
        result.predicate = predicate;

        return result;
    }

    @OnlyIn(Dist.CLIENT)
    public Component[] getDisplayNames() {
        if (getPredicate() == null) {
            return new Component[] { Component.literal("Unknown material") };
        } else if (!getItemStacks().isEmpty()) {
            return getItemStacks().stream().map(ItemStack::getHoverName).toArray(Component[]::new);
        } else if (tagLocation != null) {
            return RegistryHelper.streamTag(BuiltInRegistries.ITEM, tagLocation)
                    .map(item -> item.getName(item.getDefaultInstance()))
                    .toArray(Component[]::new);
        }

        return new Component[] { Component.literal("Unknown material") };
    }

    /**
     * The applicable items, without building a stack for any of them. Callers that run during a data
     * reload have to use this: item components are unbound while the reload runs, so constructing a
     * stack there throws.
     */
    public java.util.List<Item> getApplicableItems() {
        if (getPredicate() == null) {
            return java.util.Collections.emptyList();
        } else if (!items.isEmpty()) {
            return java.util.List.copyOf(items);
        } else if (tagLocation != null) {
            return RegistryHelper.streamTag(BuiltInRegistries.ITEM, tagLocation).collect(Collectors.toList());
        }

        return java.util.Collections.emptyList();
    }

    public ItemStack[] getApplicableItemStacks() {
        if (getPredicate() == null) {
            return new ItemStack[0];
        } else if (!getItemStacks().isEmpty()) {
            return getItemStacks().toArray(ItemStack[]::new);
        } else if (tagLocation != null) {
            return RegistryHelper.streamTag(BuiltInRegistries.ITEM, tagLocation)
                    .map(Item::getDefaultInstance)
                    .map(this::setCount)
                    .toArray(ItemStack[]::new);
        }

        return new ItemStack[0];
    }

    /**
     * A material accepting exactly the given items, built in code rather than parsed.
     *
     * The generated materials need this: they are derived from an item's own components at load
     * time, so there is no json for the deserializer to read.
     */
    public static OutcomeMaterial of(Collection<Item> items) {
        OutcomeMaterial material = new OutcomeMaterial();
        material.items = List.copyOf(items);
        material.predicate = itemStack -> material.items.contains(itemStack.getItem());
        return material;
    }

    @Nullable
    public TetraItemPredicate getPredicate() {
        return predicate;
    }

    private ItemStack setCount(ItemStack itemStack) {
        itemStack.setCount(count);
        return itemStack;
    }

    public boolean isTagged() {
        return tagLocation != null;
    }

    public boolean isValid() {
        return predicate != null;
    }

    public static class Deserializer implements JsonDeserializer<OutcomeMaterial> {

        @Override
        public OutcomeMaterial deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {
            OutcomeMaterial material = new OutcomeMaterial();

            if (element != null && !element.isJsonNull()) {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(element, "material");

                material.count = GsonHelper.getAsInt(jsonObject, "count", 1);

                if (jsonObject.has("items")) {
                    try {
                        material.items = StreamSupport.stream(GsonHelper.getAsJsonArray(jsonObject, "items", emptyArray).spliterator(), false)
                                .map(jsonElement -> GsonHelper.convertToString(jsonElement, "item"))
                                .map(Identifier::parse)
                                .map(itemId -> RegistryHelper.get(BuiltInRegistries.ITEM, itemId))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    } catch (JsonSyntaxException e) {
                        material.items = Collections.emptyList();
                    }

                    if (!material.items.isEmpty() && jsonObject.has("nbt")) {
                        try {
                            material.itemTag = TagParser.parseCompoundFully(GsonHelper.convertToString(jsonObject.get("nbt"), "nbt"));
                        } catch (CommandSyntaxException exception) {
                            throw new JsonSyntaxException("Encountered invalid nbt tag when parsing material: " + exception.getMessage());
                        }
                    }

                } else if (jsonObject.has("tag")) {
                    material.tagLocation = ItemTags.create(Identifier.parse(GsonHelper.getAsString(jsonObject, "tag")));
                }

                if (!jsonObject.has("type") && jsonObject.has("tag")) {
                    material.predicate = ItemPredicateDeserializer.deserialize(jsonObject);
                } else {
                    JsonObject copy = jsonObject.deepCopy();
                    copy.remove("count");
                    material.predicate = ItemPredicateDeserializer.deserialize(copy);
                }
            }
            return material;
        }
    }
}
