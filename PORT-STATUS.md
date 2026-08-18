# Tetra Refreshed, port status and handover

Self contained. A session with no prior context can pick this up from here.

Port of Tetra from 1.21.1 NeoForge to Minecraft 26.1.2 NeoForge, Java 25. In progress.
**1941 errors down to 1114. Does not compile yet.**

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

Before writing any fix, get the real signature. Guessing costs a wrong commit:

```bash
bash tools/jp.sh net.minecraft.world.item.ItemStack
bash tools/jp.sh net.minecraft.nbt.CompoundTag getInt
bash tools/src.sh net/minecraft/world/InteractionResult
```

`jp.sh` runs javap against the patched Minecraft jar and `forge-universal.jar` together, so it
finds NeoForge classes as well. `src.sh` prints the decompiled source from the sources jar,
which is the faster read when the question is about behaviour rather than a signature. To find
where a class moved to, list the jar:

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

## 6. What is left, 1114

The tail is flat. Measured, not assumed: 1114 errors across 236 files, the twelve worst holding
382 of them, 34 percent, and 125 files holding two or fewer for 154 total.

That shape is the point. Early wins were cascades. `getCommandSenderWorld` was 46 edits that
cleared several hundred errors, because failing to resolve it broke type inference in every
method that used it. Nothing of that shape remains among the renames.

| Count | Group |
|---|---|
| 648 | cannot find symbol, thin spread |
| 182 | signature changes surfacing as failed overrides |
| 60 | tool material system |
| 55 | model system |
| 30 | render state pipeline |
| 14 | invalid method reference |

Worst files:

```
51  client/particle/SweepingStrikeParticle.java
50  client/particle/TargetPointParticle.java
40  TetraRegistries.java
39  tools/HarvestTierRegistry.java
34  client/model/UnresolvedItemModel.java
31  client/model/ItemLayerModel.java
30  data/provider/TetraBlockStateProvider.java
24  items/modular/ItemModularHandheld.java
24  client/model/ModularOverrideList.java
21  TetraMod.java
```

Two findings worth having in writing. `LootItemFunctionType` and `LootItemConditionType` are
gone while `LootItemFunction` and `LootItemCondition` remain, so the registry wrapper types
went and registration changed shape. `ItemAbilities` still exists but `HOE_DIG`, `AXE_DIG`,
`PICKAXE_DIG` and `SHOVEL_DIG` are gone from it.

## 7. The three parts that are not renames

**Tool materials.** `Tier` and `Tiers` do not exist in 26.1.2. The tool material system was
replaced by something data driven, and the missing `ItemAbilities` dig constants above are the
same change surfacing elsewhere. Tetra is a modular tool mod whose whole premise is composing
tool behaviour, so this is a design decision about how its tiers map onto the new model, not a
lookup. `tools/HarvestTierRegistry.java` and `items/modular/ItemModularHandheld.java` are where
it lands.

**The render pipeline.** This is one change wearing three faces, and the three worst areas
after the tool materials are all it. Rendering moved to extract a render state, then submit it.

* `BlockEntityRenderer` takes two type parameters now, the entity and a `BlockEntityRenderState`,
  and the work moved from `render` into `createRenderState`, `extractRenderState` and `submit`.
  Ten renderers declare the old single parameter form.
* `TextureSheetParticle` is gone. `SingleQuadParticle` replaces it and extracts into a
  `QuadParticleRenderState`. `ParticleRenderType` is a record with four fixed constants rather
  than something a mod implements anonymously, and the immediate mode `RenderSystem` calls the
  two particle classes use, `disableCull`, `disableBlend`, `depthMask`, `setShader` and
  `setShaderTexture`, are all gone. Those two files hold 101 errors between them.
* `BakedModel`, `ItemOverrides`, `ModelState`, `IGeometryBakingContext`, `IQuadTransformer`,
  `BlockRenderDispatcher` and the whole `net.neoforged.neoforge.client.model.geometry` package
  are gone. Tetra ships a custom model loader for modular item rendering, so this is a rewrite
  against a pipeline that resolves and bakes differently. `client/model/*` is the area.

Doing these three as one piece of work will go better than three, because they share the render
state idea and the same answer to where per frame data now lives.

Leave them until the surrounding code compiles. The shape is easier to see once everything
around it is quiet.

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

**Blanket renames on a field name.** `ChunkPos.x` became private while `Vec3.x` stayed public,
so a global rewrite of `.x` breaks correct code. `tools/port_lines.py` applies a regex only to
the lines javac flagged, which is the right tool whenever a name is legal somewhere else.

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
6. Writing rules apply to every document here. No em dash, no double hyphen in prose, no
   semicolon. Check with `python ../../tools/check-writing-rules.py <file>`.

## 11. Next session, start here

1. `bash tools/port-compile.sh`, then `bash tools/port-check.sh`, and confirm it says 1114.
2. `python tools/port-show.py` and work the thin spread. 125 files hold two or fewer errors and
   are mostly one lookup each.
3. Re-measure every pass, and let `port-check.sh` decide whether the number means anything.
4. Tool materials and the render pipeline last, and do the render pipeline as one piece.
5. At 0: build, run `python ../../tools/check-mixin-targets.py` if any mixins exist, then deploy
   alongside Mutil Refreshed and launch. Mutil has never been exercised by a real consumer, so
   that launch tests both at once.
