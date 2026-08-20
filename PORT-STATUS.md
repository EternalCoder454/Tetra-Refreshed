# Tetra Refreshed, port status and handover

Self contained. A session with no prior context can pick this up from here.

Port of Tetra from 1.21.1 NeoForge to Minecraft 26.1.2 NeoForge, Java 25.
**It compiles, loads, generates a world and renders. Play testing has barely started.**

The mod loads beside 150 others in the test pack, a world creates and runs, the item models draw,
the workbench opens and a dedicated server starts clean. What has not happened is playing it: no
crafting flow, no ability, no perk and no block interaction has been exercised beyond opening a
screen. Section 12 is what to check.

## 1. Where things are

| Thing | Path |
|---|---|
| This project | `Projects\Minecraft\Mickelus Mods\Tetra Refreshed` |
| Its dependency | `..\Mutil Refreshed`, ported and building, 0 errors |
| Shared mod tooling | `..\..\tools` |
| Build and API rules | `..\..\BUILDING.md`, read section 2 first |
| Test pack | `%APPDATA%\PrismLauncher\instances\Eternally Dutified\minecraft` |

Remotes: `origin` is `EternalCoder454/Tetra-Refreshed`, `evelant` is the base, `upstream` is
`mickelus/tetra`. Work lands on branch `26.1.2`. See section 9.

## 2. The loop

```bash
bash tools/port-compile.sh
bash tools/port-check.sh
python tools/port-show.py
python tools/port-show.py ItemLayer
```

`port-compile.sh` compiles and writes `build/port/compile.log` and `build/port/errors.txt`.
Both parsers read from there, so nothing depends on a temp folder. `port-check.sh` fails the
measurement if a parse error is present, because a parse error aborts analysis and collapses
the count into something that reads like near success. Run it before believing any number.
`port-show.py` prints every error with its source line, optionally filtered by a path fragment.

```bash
bash tools/run.sh          # build, deploy to the test pack, launch, say whether it loaded
./gradlew.bat runServer    # dedicated server, no gui, exercises data loading on its own
./gradlew.bat runClient -PquickPlay=<world>
```

`run.sh` is the honest check. A compiling jar said nothing about whether the mod loads, and every
failure after the first zero errors surfaced only by launching. It kills the stale javaw that holds
the jar open, because a crashed instance keeps a lock and the copy silently fails otherwise.

`portedit.py` applies literal replacements to a source file without disturbing its line endings,
and fails loudly if a search string does not match exactly once. Every Java file here is CRLF, so a
multi line replacement written with `\n` silently matches nothing otherwise. Use it from a script
file rather than a heredoc, for the reason in section 8.

Before writing any fix, get the real signature. Guessing costs a wrong commit:

```bash
bash tools/jp.sh net.minecraft.world.item.ItemStack
bash tools/jp.sh net.minecraft.nbt.CompoundTag getInt
bash tools/src.sh net/minecraft/world/InteractionResult
```

`jp.sh` runs javap against the whole compile classpath, read from `build/port/cp.txt`, which
`tools/cp.gradle` writes from the configuration Gradle actually compiles against:

```bash
./gradlew.bat -I tools/cp.gradle dumpCp
```

Asking the build rather than globbing the cache matters twice over. An older `forge-universal.jar`
from another project's cache answered confidently and wrongly, which is why `Capabilities` read as
an empty class for a while. And `FMLEnvironment` is not in NeoForge at all, it is in the
fancymodloader jar, so a glob over the two obvious jars reports it missing when it is right there
on the classpath. `src.sh` prints the decompiled source from the sources jar, which is the faster
read when the question is about behaviour rather than a signature. To find where a class moved to,
list the jar:

```bash
unzip -l build/moddev/artifacts/minecraft-patched-26.1.2.95-merged.jar | grep VillagerTrades
```

## 3. Base and toolchain

Upstream Tetra's newest branch is `1.20`. This does not start from it. It starts from PR 931,
"Port to 1.21.1 neoforge" by evelant, head `evelant/tetra` branch `1.21` at `8aa22195`, open
and unmerged, 884 files changed. That turns a 1.20 to 26.1.2 jump into a 1.21.1 to 26.1.2 one,
the same jump Mutil Refreshed already made. 792 Java files.

Toolchain is the set Duty is known to build with. Gradle 9.6.1, ModDevGradle 2.0.144, Java 25,
NeoForge 26.1.2.95, Parchment 1.21.11 with 2025.12.20 mappings.

Mutil Refreshed is published to mavenLocal as `se.mickelus.mutil:mutil:26.1.2-7.0.0-pre.0`.
Tetra resolves mutil by maven coordinate, so if mutil is rebuilt, republish it or Tetra stops
resolving:

```bash
cd "../Mutil Refreshed" && ./gradlew.bat publishToMavenLocal
```

## 4. Progress

| Stage | Unique errors |
|---|---|
| First uncapped compile | 1941 |
| ResourceLocation, GuiGraphics, isClientSide, critereon, FastColor | 1601 |
| Package moves and merged result types | 1601 |
| getCommandSenderWorld, sidedSuccess, hasShiftDown, getNormal | 1475 |
| RenderType, ArmorMaterial, MethodsReturnNonnullByDefault | 1451 |
| NBT Optional getters, InteractionResult switches, cooldowns | 1257 |
| Item.use, Inventory, Font, villager packages | 1186 |
| Block entity and entity storage | 1114 |
| Particles onto the render state pipeline | 970 |
| Tool materials and dig abilities | 879 |
| Shape updates, tool rules, materials | 850 |
| Gui overlays and the mutil packet api | 788 |
| Screens, block entity types, ToggleableSlot | 758 |
| Loot registries and item predicates | 734 |
| Mouse input, damage, entity spawning | 700 |
| Character input, sounds, clone stacks | 718 |
| Sound holders corrected | 679 |
| Access transformer, particle colours, permissions | 663 |
| Packet payload types, fixed in mutil | 638 |
| Overlay messages, toasts, command permissions | 632 |
| Spelling, registry lookups, modifier keys | 627 |
| appendHoverText onto the tooltip consumer | 603 |
| Light dampening and comparator output | 600 |
| Block removal, effect ticks, item release | 578 |
| Item classes to components and tags | 555 |
| Gui draw calls onto the render pipeline | 527 |
| Screens onto extract and submit | 500 |
| Every block entity and entity renderer | 402 |
| Block state datagen onto vanilla generators | 370 |
| Block tooltips onto BlockTooltip | 353 |
| Toasts, key mappings, key events | 336 |
| ItemModularHandheld finished | 328 |
| Immediate mode render state, tags provider | 308 |
| Trades package removed | 293 |
| Item transfer api, block and item hooks | 261 |
| Registry api, reload listeners, entity spawning | 231 |
| Gui, containers, loot, time | 200 |
| Datagen, block entity packets, debug rendering | 183 |
| Shield disabling, drip particles, joml codecs | 166 |
| Block outline overlay | 161 |
| The custom item model rebuilt | 25 |
| Shield renderer and tag mixins | **0** |

`javac` caps error output. Early figures were capped at 100 and then 2000 and understated the
real count. `-Xmaxerrs 20000` is set in `build.gradle` now, so 1941 is the first honest number.

## 5. Renames already applied

Each confirmed with javap, not guessed.

| From | To | Sites |
|---|---|---|
| `ResourceLocation` | `Identifier` | 485 |
| `isClientSide` field | `isClientSide()` | 140 |
| `ItemInteractionResult`, `InteractionResultHolder` | `InteractionResult` | 105 |
| NBT getters | `getIntOr`, `getStringOr`, `getCompoundOrEmpty` and friends | 102 |
| `GuiGraphics` | `GuiGraphicsExtractor` | 68 |
| `getCommandSenderWorld()` | `level()` | 46 |
| `InteractionResult.sidedSuccess(x)` | `InteractionResult.SUCCESS` | 30 |
| `advancements.critereon` | `advancements.criterion` | 25 |
| `ChunkPos.x`, `ChunkPos.z` fields | `x()`, `z()` | 24 |
| `UseAnim` | `ItemUseAnimation` | 22 |
| `Screen.hasShiftDown()` | `Minecraft.getInstance().hasShiftDown()` | 21 |
| `addCooldown(Item, n)` | `addCooldown(ItemStack, n)` | 20 |
| `getNormal()` | `getUnitVec3i()` | 18 |
| `DirectionProperty` | `EnumProperty<Direction>` | 18 |
| `FastColor.ARGB32` | `ARGB` | 18 |
| `Inventory.items`, `.selected`, `.offhand` | accessors, offhand via `getOffhandItem()` | 14 |
| `Level.random` field | `getRandom()` | 11 |
| `RenderType` | `client.renderer.rendertype.RenderType` | 9 |
| `Material` | `client.resources.model.sprite.Material` | 9 |
| `Font.drawInBatch` | trailing bidirectional flag dropped | 9 |
| `AbstractArrow` | `world.entity.projectile.arrow.AbstractArrow` | 7 |
| `VillagerTrades` | `world.item.trading.VillagerTrades` | 5 |
| `ItemTransforms` | `client.resources.model.cuboid.ItemTransforms` | 4 |
| `MethodsReturnNonnullByDefault` | removed from Minecraft, dropped | 4 |
| `ArmorMaterial` | `world.item.equipment.ArmorMaterial` | 2 |
| `VillagerProfession` | `world.entity.npc.villager.VillagerProfession` | 2 |
| `Tier`, `Tiers`, `SimpleTier` | `ToolMaterial` | 45 |
| `ItemAbilities.*_DIG` | `TetraItemAbilities`, same interned names | 38 |
| `TextureSheetParticle` | `SingleQuadParticle` | 2 files, 101 errors |
| `LayeredDraw.Layer` | `neoforge.client.gui.GuiLayer` | 11 |
| `SoundEvents` holders | `.value()`, only the 153 that are holders | 39 |
| `LootItemFunctionType`, `LootItemConditionType` | the `MapCodec` itself | 12 |
| `ItemSubPredicate` | `DataComponentPredicate` | 8 |
| `mouseClicked(x, y, button)` | `mouseClicked(MouseButtonEvent, boolean)` | 7 |
| `charTyped(char, int)` | `charTyped(CharacterEvent)` | 3 |
| `MobSpawnType` | `EntitySpawnReason` | 4 |
| `Entity.moveTo` | `snapTo` | 5 |
| `ResourceKey.location()` | `identifier()` | 4 |
| `Direction.getNearest(d, d, d)` | `getApproximateNearest` | 4 |
| `net.minecraft.Util` | `net.minecraft.util.Util` | 2 |

Two API changes were not renames but were still mechanical, and are done.

**NBT.** `CompoundTag` and `ListTag` getters return `Optional`. Each has an `*Or` sibling
taking the default the old getter returned implicitly. Arity is what separates a tag read from
an unrelated method of the same name, so the rewriter in `tools/port_nbt.py` balances parens
rather than matching a name. `Component.getString()` takes none and
`StringArgumentType.getString(ctx, name)` takes two, and neither is a tag read. Typed
`contains(key, TAG_COMPOUND)` lost its type argument, which the `Optional` getters now carry.

**Storage.** Block entity and entity persistence no longer passes a `CompoundTag` and a
registry provider. `loadAdditional`, `saveAdditional`, `loadWithComponents`,
`removeComponentsFromTag` and the two `Entity` save hooks take a `ValueInput` or `ValueOutput`,
which carries the registry context itself. `ItemStackHandler` is a `ValueIOSerializable`, so
inventories become `readChild` and `putChild`. Stacks go through `ItemStack.CODEC` and
`ItemStack.MAP_CODEC`. Lists become typed lists under the keys they already used, so the stored
format did not move. The structure processors in `levelgen` share the block entity write
helpers, so those helpers took `ValueOutput` and each processor wraps a `TagValueOutput` over
the registry access it already held and merges the built tag back in. Update packets still
carry a `CompoundTag` over the wire, so those readers wrap it in a `TagValueInput`.

## 6. What is left

Nothing, as measured by the compiler. `bash tools/port-compile.sh` reports 0 errors,
`tools/port-check.sh` confirms the number is comparable, `./gradlew.bat build` succeeds, and
`tools/check-at.py` and `tools/check-mixin-targets.py` both come back clean.

What is left is everything a compiler cannot see. **The mod has never been launched.** Section 7
lists the behaviour that changed on the way here and section 11 is the order to check it in.

## 7. What changed in behaviour, not just in signature

Every item here compiles. Each one is a place where the new API could not express what the old code
did, so the port made a decision. Read this before deciding a bug is a regression.

**Villager trades, removed and needing readding as data.** `VillagerTrades.ItemListing` is gone,
NeoForge's `VillagerTradesEvent` and `WandererTradesEvent` no longer exist, and trades are registry
data. Tetra's `trades` package had nothing left to plug into, so it is deleted rather than left as
code that reads as live and never runs. **Tetra currently sells nothing.** The five classes are in
git at the commit that removed them, which is where the full table of professions, levels, items
and prices lives.

Re adding them is data, not code. One file per trade under
`data/tetra/villager_trade/<name>.json`:

```json
{ "wants": { "id": "minecraft:emerald", "count": 4 },
  "gives": { "id": "tetra:scroll", "count": 1 },
  "max_uses": 1.0, "xp": 5, "reputation_discount": 0.05 }
```

and then each one appended to the vanilla profession level tag it belongs in, at
`data/minecraft/tags/villager_trade/<profession>/level_<n>.json` with `"replace": false`. The
wandering trader uses `wandering_trader/common` and `uncommon` the same way. `additional_wants`
carries the second cost the scrap trades charged.

The scroll trades are the fiddly ones: `ScrollItem.hammerEfficiency` and friends are stacks built
with scroll data components, so their `gives` needs the component patch spelled out rather than just
an item id.

**The interactive block overlay draws again.** It was written off during the port as needing a
redesign of mutil's gui layer, and that was wrong on the important point. NeoForge ships
`ExtractBlockOutlineRenderStateEvent.addCustomRenderer`, and a `CustomBlockOutlineRenderer` is
handed a `PoseStack` and a `MultiBufferSource.BufferSource`, which are exactly the two things
`RenderHighlightEvent.Block` used to carry. The draw phase was never missing, only the place to
register for it.

What the gui layer needed was smaller than a redesign. `GuiElement` grew a `drawWorld` recursion
beside `draw`, using the same attachment offsets so an element sits where it would on screen, and
only the two leaves this overlay reaches had to implement it: `GuiTexture` emits a quad through
`RenderTypes.entityTranslucent`, and `GuiString` uses `Font.drawInBatch`, with `GuiStringOutline`
using `drawInBatch8xOutline` rather than drawing itself nine times on a block face. `GuiRootHud`
keeps one copy of the face transform and both paths call it.

Play tested and correct, with one change made after looking at it. The hints waited 500ms and 650ms
before fading in, which upstream does so they do not flash while the crosshair sweeps across blocks.
That reason is a good one and the stagger keeps it, but half a second reads as lag rather than as
restraint, so the two delays are 150ms and 200ms now. They are named constants in
`InteractiveOutlineGui`, and the tool icon reads the second one rather than carrying its own copy.

That is a deliberate divergence from upstream Tetra, and the only one in the overlay.

**Stored inventories do not carry over.** The item capability is a `ResourceHandler<ItemResource>`
now and nothing adapts an `IItemHandler` to it, so the workbench, the rack and the forged container
hold an `ItemStacksResourceHandler` and expose the old interface through `IItemHandler.of`. That
handler writes under `stacks` where `ItemStackHandler` wrote `Items` and `Size`, so a world saved
before this port loses those three inventories' contents. Nothing else about the format moved.

**Transfer units and core extractors recompute more often.** `neighborChanged` takes an
`Orientation` rather than the position the change came from, and vanilla passes null for it on
ordinary neighbour updates, so the guard that skipped the block a unit outputs into cannot be
reconstructed. `updateTransferState` runs unconditionally instead. It reaches the same state, and
`setSending` and `setReceiving` write with `UPDATE_CLIENTS` alone, so it cannot feed back.

**Modular items cannot be enchanted from a book at an anvil, by omission.**
`IItemExtension.isBookEnchantable` is gone and Tetra returned false from it, so the restriction it
expressed is simply absent now. Enchantability itself survived: `Item#getEnchantmentValue` is gone
too, so `ModularItemComponentHelper` mirrors the per stack value into the `ENCHANTABLE` component
beside the `TOOL` and `MAX_DAMAGE` ones it already syncs.

**A modular item's components sync on the first tick rather than on load.**
`Item#verifyComponentsAfterLoad` is gone, and `inventoryTick` is server only now and names the slot
the stack sits in rather than passing a selected flag, so the load time sync moved there.

**Shieldbreaker reads the attacker's weapon.** `LivingEntity#canDisableShield` is gone. Whether an
attack disables blocking is the attacking weapon's `disableBlockingForSeconds` now, so the effect
tests that instead of the old hook.

**A material outside vanilla's set can no longer be named in data.** `ArmorMaterial` stopped being a
registry entry and became a plain record, so there is no registry to resolve an id against.
`ArmorMaterialDeserializer` maps the ids that used to name the vanilla materials onto the
`ArmorMaterials` constants.

**The drip particles no longer animate their sprite.** Vanilla's `DripParticle` subclasses take a
sprite chosen up front rather than one picked from the particle's age, which is how vanilla builds
its own now.

**The moon phase is read for the dimension rather than a position.** It became a positional
environment attribute, and `TimeNumberProvider` has no position, so it reads the dimension value.
`Level#getDayTime` became the dimension's own clock.

## 7a. The item model system, rebuilt

This was the largest single piece and is worth understanding before touching item rendering.

NeoForge's `net.neoforged.neoforge.client.model.geometry` package is gone, and with it `BakedModel`,
`ItemOverrides`, `IGeometryBakingContext`, `IQuadTransformer`, `ModelState` and `IUnbakedGeometry`.
There is no baked model left to swap per stack. Picking geometry per stack is what
`ItemModel#update` does, which is why `ModularOverrideList`, `BakingContextWrapper`,
`ItemLayerModel`, `TetraSeparateTransformsModel`, `ColorQuadTransformer` and `QuadTransformerBuilder`
all collapsed into one class, `client/model/ModularItemModel.java`.

A module model is one `LayerRenderState` now. That is a better fit than the old single baked model:
a layer carries its own local transform, tint list and item transform, so the per layer work that
needed quad transformers is state on the layer. Only emissivity still reaches into the quads,
because it lives on a quad's `MaterialInfo`. The per display context filtering that
`TetraSeparateTransformsModel` did happens in `update`, which is handed the context it draws for.

**The piece that was not obvious.** `UnbakedGeometryHelper.createUnbakedItemElements`, which turned
a module's sprite into extruded item quads, went with the package. Vanilla still does exactly that
job in `client.resources.model.cuboid.ItemModelGenerator.bake`, which is private, as is the only
constructor that builds the `TextureSlots` it wants. Both are widened in
`src/main/resources/META-INF/accesstransformer.cfg` and checked by `tools/check-at.py`.

Item colours and item model properties moved the same way, from a handler bound to an item to a
registered type named in the model json. `ItemColor` became `ItemTintSource` (`ScrollItemColor`),
and `ItemProperties.register` became `RangeSelectItemModelProperty` (`ScrollMaterialProperty`,
`CellChargedProperty`) or `ConditionalItemModelProperty` (`HandheldStateProperty`, which covers the
shield's blocking and throwing states in one type).

`BlockEntityWithoutLevelRenderer` is gone and `IClientItemExtensions` no longer hands out a custom
renderer, so the shield is a `SpecialModelRenderer`, registered by id and submitting into the render
pipeline rather than drawing immediately. It no longer registers itself as a reload listener,
because it is rebaked from its unbaked form whenever models reload.

Display transforms have no vanilla codec, because they belong to a block model and reach an item
model through `ModelRenderProperties`. Tetra needs them named per transform variant, which a block
model has no room for, so `TransformCodecs` reads them on Tetra's side and the model files keep the
`display` and `variants` shape they already had.

**The resource layout moved with it.** An item's model used to be
`assets/tetra/models/item/<id>.json`, found by name. It is `assets/tetra/items/<id>.json` now, and
it names a model type rather than being one. The block models under `models/item` keep their
geometry and display transforms.


**Anything that builds an ItemStack while data or registration is running.** Item data components
bind at one point and rebind on every datapack reload, and constructing a stack outside that window
throws `NullPointerException: Components not bound yet`, which fails the whole mod. This caught the
enchantment aspects during mod construction, the crossbow dummy and the vanilla bow during item
registration, the scroll's creative stacks on FMLCommonSetupEvent, and the material and replacement
deserializers during a datapack reload. Deferring the build is not always enough: SchematicRegistry
forced the material's lazy build from inside the same reload, so it asks for items rather than
stacks now. Hold the item, build the stack when something renders or uses it.

**A Properties cannot be shared between two blocks or items.** It carries the registry id now and
setId mutates it, so a shared constant gives every block the last id written. ForgedBlockCommon and
the multiblock builder both did this. DeferredRegister.Blocks and .Items build one per entry, which
is why every constructor takes its Properties rather than making its own.

**@OnlyIn does not strip anything from mod classes.** Naming a client type in a common class leaves
a reference in its bytecode, and the class then fails to load on a dedicated server and takes
everything on it down. The holosphere and scroll screens, both menu factories, the input handlers
and mutil's PacketHandler all did this. Move the client code to its own class, or widen the
parameter if the body never needed the client type, as LungeEffect did.

**A tint written without an alpha channel reads as transparent.** Tetra has always taken an alpha of
zero to mean opaque, and the quad transformer that did the fix was one of the classes the item model
rebuild deleted. Every module whose tint omits the alpha byte then draws nothing. The symptom was an
obsidian hammer showing its heads and not its handle, because the two modules written as ffffffff
were the only ones coming through.

**A display transform's translation is in sixteenths.** Vanilla scales it by 0.0625 when reading a
model. Tetra reads its own, and passing the raw value through put a held item metres from the hand.

**An optional dependency has to be guarded at runtime.** curios is compileOnly and optional, and the
pack does not ship it, so touching CuriosApi unconditionally crashed on the first player tick. Put
the api behind a nested class so resolving the guard does not load it.

**The published curios jar is built for 1.21.1** and refuses to load on 26.1.2, which stops
runClient and runServer before they start. It is dropped from the dev runtime only.


## 8. Traps already hit, do not repeat

**Greedy regex on nested parens.** This has now bitten twice, in both directions. A pattern
ending in an unescaped character class that stops at the first close paren will cut a nested
call such as `isClientSide()` in half and leave a dangling paren. That turned 23 files into
parse errors, and because parse errors abort analysis the count read **41** and looked like
near success when the real figure was 1496. Making the match non greedy does not fix it. A non
greedy `[^;]*?\)` against `setAction(new InteractionResult<>(SUCCESS, event.getBow()))` stops
at the inner call and leaves a spare paren the same way, and the count read **3**. Balance
parens in code, as `tools/port_nbt.py` does, or scope the substitution to the flagged lines, as
`tools/port_lines.py` does. Then run `tools/port-check.sh` before believing the number.

**Substring collision in properties.** Replacing `minecraft_version=1.21.1` also hit
`parchment_minecraft_version=1.21.11`, producing `26.1.21`. Anchor on line starts.

**Heredoc escape corruption.** Bash eats backslash escapes in heredocs. A backslash b becomes a
literal backspace byte and a backslash n becomes a newline. Write the script to a file and run
the file. This bit three separate times across the wider project, and a fourth time here, where
a quoted heredoc carrying a large Python script failed to parse at the shell level and produced
nothing at all.

**Guessing a replacement.** `Window.isShiftDown` does not exist, it is `Minecraft.hasShiftDown`.
`EffectsInInventory` is not the replacement for `EffectRenderingInventoryScreen`. Run javap
first, every time.

**A stale access transformer fails silently.** An entry naming a class or member that moved is
ignored rather than failing the build, so the widening never happens and the failure surfaces
much later as a private access error on a line that looks unrelated. Nine of the sixteen entries
here were stale, four of them still written in SRG names and therefore dead since before this
port. `tools/check-at.py` checks every entry against the jar. Run it after any package move.

**Assuming a constant holder is uniform.** `SoundEvents` is mixed. 153 of its constants are a
`Holder.Reference<SoundEvent>` and 1625 are a plain `SoundEvent`, so a blanket `.value()` was
wrong in both directions and cost 39 errors. Drive the edit from javap output, not from the
name.

**Reading a rising count as a regression.** Resolving a symbol lets javac finish analysing a
file it had given up on, and it then reports errors that were always there. One pass here went
700 to 718 while clearing six groups outright. Compare the groups, not the total.

**Blanket renames on a field name.** `ChunkPos.x` became private while `Vec3.x` stayed public,
so a global rewrite of `.x` breaks correct code. `tools/port_lines.py` applies a regex only to
the lines javac flagged, which is the right tool whenever a name is legal somewhere else.

**Building an ItemStack during mod construction.** Item data components are bound after mods are
constructed, so `new ItemStack(item)` in a constructor or in anything a mod constructor calls throws
`NullPointerException: Components not bound yet`, which fails the whole mod before any other mod
loads. This is the first runtime failure the port hit, in `TetraEnchantmentHelper.init`, which
`TetraMod.<init>` calls directly and which built stacks for every enchantment aspect. It now holds
the items and builds the stacks on first use. Anything reached from `FMLCommonSetupEvent` or later
is safe, which is why `ScrollItem.commonInit` building fifteen of them is not a problem. When in
doubt, build the stack lazily.

**A for loop over paths with a space in them.** `for j in $(find "$HOME/.gradle/..." ...)` splits
`C:\Users\Zachary Smith\...` on the space and searches two paths that do not exist, silently. That
is how `FMLEnvironment` read as absent from every jar in the cache when it was sitting in the
fancymodloader jar the build resolves. `tools/jp.sh` reads `build/port/cp.txt` now, which
`tools/cp.gradle` writes from the configuration Gradle actually compiles against. Ask the build,
do not guess, and do not iterate over paths as words.

**A multi line replacement that matches nothing.** Every Java file here is CRLF. Reading one with
the line endings preserved and then searching for a block written with `\n` finds nothing, and a
`str.replace` that matches nothing returns the string unchanged and reports no error, so the edit
reads as applied and did nothing. `tools/portedit.py` reads with universal newlines, matches
against `\n`, asserts the match count, and writes back in the file's original ending. Use it for
anything spanning more than one line.


## 9. Repo status

`EternalCoder454/Tetra-Refreshed` is a fork. The API reports `fork: true`, parent
`mickelus/tetra`. The earlier blocker, that it existed as a plain repository and had to be
deleted and recreated with the fork button, is resolved.

`origin` now points at it and branch `26.1.2` is pushed. The local branch was renamed from
`1.21` to `26.1.2` so it names the target rather than the base. The fork's default branch is
still `1.20`, which is upstream's newest, so nothing was overwritten.

Tetra is not open source. Its README carries custom terms, copyright 2018 to 2024 Mikael
Eriksson Vikner, and there is no LICENSE file at the repository root. Those terms permit
forking and modifying, which is what this is. They prohibit redistributing the project in its
entirety as source or compiled code, and prohibit code 50 percent or more functionally
equivalent, so a standalone public repository stays out of the question. Keep the work on the
fork.

The terms also restrict the Perk system specifically. Do not change, reuse or remove perk
content or assets.

## 10. Rules

1. Minecraft 26.1.2, NeoForge only. Java 25.
2. Credit Mikael Eriksson Vikner as author. Credit **EternalHell** for the port only, never as
   author, and never a real name.
3. Keep every upstream copyright header.
4. Never mix a port and a bug fix in one commit. A port that also changes behaviour cannot be
   reviewed against upstream, which is the whole value of staying a fork.
5. Upstream issues get fixed after the port, not during.
   See <https://github.com/mickelus/tetra/issues>.
6. Writing rules apply to every document here. `check-writing-rules.py --rules` from [mc-tools](https://github.com/EternalCoder454/mc-tools)
   states them and nothing else does.

## 11. Next session, start here

The compiler, the loader and the renderer are all happy. What is left is playing it.

`.github/workflows/boot-gate.yml` runs the same gate on every push to this branch, so the list
below is what to do locally rather than what nobody is watching. It builds all five repositories,
EMI Refreshed included, and keeps the server log as an artifact when it fails.

1. `python tools/boot-gate.py` first. It boots the dedicated server, loads the world and fails
   on anything of ours that was dropped, which is the check that used to be done by reading a log
   by hand. It passes clean as of the last commit. Then `bash tools/run.sh` for the client half,
   which is the only place texture and atlas failures appear.
2. Craft something. The workbench opens and modular items render, but no crafting flow, ability,
   perk or block interaction has been exercised. Every one of those is untested.
3. Section 7 lists the behaviour that changed on the way here. Each entry is a decision the port
   made that a compiler cannot check, and villager trades are missing outright.
4. `./gradlew.bat runServer` for anything server shaped. It starts clean and is far faster to read
   than the pack.

## 11a. Four attribute names resolved to nothing

`AttributesDeserializer` warns when a key names no registered attribute, and the launch log carried
30 of them. Each was a real modifier being discarded in silence before that warning existed.

| key in data | count | now maps to | why |
|---|---|---|---|
| `**tetra:draw_damage` | 14 | `tetra:draw_strength` | a name never registered, in this mod's own quality and ravenous improvements |
| `generic.movement_speed` | 10 | `minecraft:movement_speed` | the generic prefix went in 1.21, and this one was missing from the map |
| `art_of_forging:beheading` | 4 | nothing, it is data | an ItemEffect sitting in an attributes block. Fixed in Secrets of Forging |
| `tetra.draw_speed` | 2 | `tetra:draw_speed` | written with a dot, which parses as a path in the minecraft namespace |

`draw_damage` is worth a note. It appears beside `generic.attack_damage` and `tetra:ability_damage`
as the ranged third of a damage triple, and `draw_strength` is the attribute `ModularBowItem` reads
for bow damage. Nothing in the java has ever named `draw_damage`, so quality and ravenous have never
raised a bow in this mod's history.

The zero byte `stonecutter/held.png` is deleted. Nothing in data, models or java referenced it, and
the atlas scanned the directory and failed on it every load.

**Five block entity blocks were drawing twice.** Every block whose renderer draws it returned
`RenderShape.ENTITYBLOCK_ANIMATED` before the port, which kept the chunk mesher from baking a static
model. That constant does not exist in 26.1.2, where the enum is `INVISIBLE` and `MODEL`, so the
override was dropped rather than mapped and the blocks fell back to `MODEL`. The static cube and the
renderer's cube then drew in the same place, and the faces flickered against each other. The forge
hammer was the loudest, being two blocks doing it at once.

`INVISIBLE` restores it. Not a particle only model, which is how vanilla does chests, because the
item models here inherit from the block models and emptying those would take the held items with
them.

Affected: `HammerBaseBlock`, `HammerHeadBlock`, `ForgedContainerBlock`, `CoreExtractorPistonBlock`,
`ScrollBlock`.

**Three deliberate changes to how the forged blocks behave.** All three diverge from upstream and
are wanted rather than accidental.

Materials leave the workbench when the screen closes. They used to sit in the slots until something
else emptied them, which happens when the schematic or the slot changes, so a stack put in and
walked away from stayed there until the next visit and read as lost. The tool in slot zero stays,
which is the point of a workbench holding one.

The forged workbench and both hammer blocks are mineable with a pickaxe now, not only a Tetra
hammer. They were already in `needs_netherite_tool`, so the tier was always netherite, but nothing
vanilla counted as a correct tool and a pickaxe got no drops however long it took.

Breaking them correctly returns the block instead of scrap, so a workbench or a hammer can be
moved. Upstream gave the block back only with silk touch and scrap otherwise. The old
`forged_workbench_break` and `hammer_break` tables are left in place, unreferenced, rather than
deleted.

One item places both hammer blocks, so breaking either now takes the other down with it, removed
without dropping. Without that, both halves drop the item and one hammer becomes two.

## 12. Known remaining

* Villager trades are gone and need readding as data. Section 7.
* Stored inventories from before this port do not load. Section 7.
* TetraBlockStateProvider writes `models/item` but not `items`, so the 38 generated item model
  entries beside them are written by hand and a runData would not recreate them.
