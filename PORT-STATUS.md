# Tetra Refreshed, port status and handover

Self contained. A session with no prior context can pick this up from here.

Port of Tetra from 1.21.1 NeoForge to Minecraft 26.1.2 NeoForge, Java 25. In progress.
**1941 errors down to 1451. Does not compile yet.**

## 1. Where things are

| Thing | Path |
|---|---|
| This project | `Projects\Minecraft\Mickelus Mods\Tetra Refreshed` |
| Its dependency | `..\Mutil Refreshed`, ported and building, 0 errors |
| Shared mod tooling | `..\..\tools` |
| Build and API rules | `..\..\BUILDING.md`, read section 2 first |
| Test pack | `%APPDATA%\PrismLauncher\instances\Eternally Dutified\minecraft` |

Remotes here: `evelant` is the base, `upstream` is `mickelus/tetra`. There is deliberately no
`origin`. See section 9.

## 2. The loop

```bash
bash tools/port-compile.sh
python tools/port-show.py
python tools/port-show.py ItemLayer
```

`port-compile.sh` compiles and writes `build/port/compile.log` and `build/port/errors.txt`.
Both parsers read from there, so nothing depends on a temp folder. `port-show.py` prints every
error with its source line, optionally filtered by a path fragment.

Before writing any fix, get the real signature. Guessing costs a wrong commit:

```bash
JAVAP="/c/Program Files/Java/jdk-25.0.4/bin/javap.exe"
MC=$(python ../../tools/_minecraft_jar.py)
"$JAVAP" -p -s -cp "$MC" net.minecraft.world.item.ItemStack
```

NeoForge classes are not in that jar. They are in `forge-universal.jar`:

```bash
UJ=$(find "$HOME/.gradle/caches" -name "forge-universal.jar" | head -1)
"$JAVAP" -p -cp "$UJ" net.neoforged.neoforge.common.ItemAbilities
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

`javac` caps error output. Early figures were capped at 100 and then 2000 and understated the
real count. `-Xmaxerrs 20000` is set in `build.gradle` now, so 1941 is the first honest number.

## 5. Renames already applied

Each confirmed with javap, not guessed.

| From | To | Sites |
|---|---|---|
| `ResourceLocation` | `Identifier` | 485 |
| `isClientSide` field | `isClientSide()` | 140 |
| `ItemInteractionResult`, `InteractionResultHolder` | `InteractionResult` | 105 |
| `GuiGraphics` | `GuiGraphicsExtractor` | 68 |
| `getCommandSenderWorld()` | `level()` | 46 |
| `InteractionResult.sidedSuccess(x)` | `InteractionResult.SUCCESS` | 30 |
| `advancements.critereon` | `advancements.criterion` | 25 |
| `UseAnim` | `ItemUseAnimation` | 22 |
| `Screen.hasShiftDown()` | `Minecraft.getInstance().hasShiftDown()` | 21 |
| `getNormal()` | `getUnitVec3i()` | 18 |
| `DirectionProperty` | `EnumProperty<Direction>` | 18 |
| `FastColor.ARGB32` | `ARGB` | 18 |
| `RenderType` | `client.renderer.rendertype.RenderType` | 9 |
| `Material` | `client.resources.model.sprite.Material` | 9 |
| `AbstractArrow` | `world.entity.projectile.arrow.AbstractArrow` | 7 |
| `ItemTransforms` | `client.resources.model.cuboid.ItemTransforms` | 4 |
| `MethodsReturnNonnullByDefault` | removed from Minecraft, dropped | 4 |
| `ArmorMaterial` | `world.item.equipment.ArmorMaterial` | 2 |

## 6. What is left, 1451

The tail is flat. Measured, not assumed: 1451 errors across 256 files, the twelve worst holding
409 of them, 28 percent, and 121 files holding two or fewer for 152 total.

That shape is the point. Early wins were cascades. `getCommandSenderWorld` was 46 edits that
cleared several hundred errors, because failing to resolve it broke type inference in every
method that used it. Nothing of that shape remains. The last pass moved 1475 to 1451, so about
25 errors per distinct API change. Expect sixty to a hundred more individual resolutions.

| Count | Group |
|---|---|
| 722 | cannot find symbol, thin spread |
| 209 | signature changes surfacing as failed overrides |
| 73 | `CompoundTag` getters return `Optional` now |
| 71 | `InteractionResult` is an interface, not an enum |
| 60 | fields became accessors |
| 48 | model system |
| 47 | tool material system |
| 30 | `BlockEntity` load and save signatures |
| 24 | GUI and render layers |

Worst files:

```
51  client/particle/SweepingStrikeParticle.java
50  client/particle/TargetPointParticle.java
40  TetraRegistries.java
39  tools/HarvestTierRegistry.java
36  items/modular/ItemModularHandheld.java
34  client/model/UnresolvedItemModel.java
31  client/model/ItemLayerModel.java
30  data/provider/TetraBlockStateProvider.java
```

Two more findings worth having in writing. `LootItemFunctionType` and `LootItemConditionType`
are gone while `LootItemFunction` and `LootItemCondition` remain, so the registry wrapper types
went and registration changed shape. `ItemAbilities` still exists but `HOE_DIG`, `AXE_DIG`,
`PICKAXE_DIG` and `SHOVEL_DIG` are gone from it.

## 7. The two parts that are not renames

**Tool materials.** `Tier` and `Tiers` do not exist in 26.1.2. The tool material system was
replaced by something data driven, and the missing `ItemAbilities` dig constants above are the
same change surfacing elsewhere. Tetra is a modular tool mod whose whole premise is composing
tool behaviour, so this is a design decision about how its tiers map onto the new model, not a
lookup. `tools/HarvestTierRegistry.java` and `items/modular/ItemModularHandheld.java` are where
it lands.

**Models.** `BakedModel`, `ItemOverrides`, `ModelState`, `IGeometryBakingContext`,
`IQuadTransformer`, `BlockRenderDispatcher` and the whole
`net.neoforged.neoforge.client.model.geometry` package are gone. Tetra ships a custom model
loader for modular item rendering. This is a rewrite against a pipeline that resolves and bakes
differently. `client/model/*` is the area.

Also `InteractionResult` is an interface now, so every `switch` over it fails. Those need
reading, not substituting.

Leave both until the surrounding code compiles. The shape is easier to see once everything
around it is quiet.

## 8. Traps already hit, do not repeat

**Greedy regex on nested parens.** A pattern ending in an unescaped character class that stops
at the first close paren will cut a nested call such as `isClientSide()` in half and leave a
dangling paren. That turned 23 files into parse errors. Parse errors abort analysis, so the
count read **41** and looked like near success when the real figure was 1496. If a number drops
implausibly far in one pass, suspect parse errors before believing it.

**Substring collision in properties.** Replacing `minecraft_version=1.21.1` also hit
`parchment_minecraft_version=1.21.11`, producing `26.1.21`. Anchor on line starts.

**Heredoc escape corruption.** Bash eats backslash escapes in heredocs. A backslash b becomes a
literal backspace byte and a backslash n becomes a newline. Write the script to a file and run
the file. This bit three separate times across the wider project.

**Guessing a replacement.** `Window.isShiftDown` does not exist, it is `Minecraft.hasShiftDown`.
`EffectsInInventory` is not the replacement for `EffectRenderingInventoryScreen`. Run javap
first, every time.

## 9. Repo blocker, resolve before pushing anything

`EternalCoder454/Tetra-Refreshed` exists but is **not a fork**. The API reports
`fork: false, parent: null`.

Tetra is not open source. Its README carries custom terms, copyright 2018 to 2024 Mikael
Eriksson Vikner, and there is no LICENSE file at the repository root. Those terms permit forking
and modifying. They prohibit redistributing the project in its entirety as source or compiled
code, and prohibit code 50 percent or more functionally equivalent. A full port in a standalone
public repository is both of those.

A plain repository cannot be converted to a fork. It has to be deleted and recreated with the
fork button on `mickelus/tetra`.

Until then, local work only. Forking and modifying is permitted and nothing has been published.
Do not add an `origin` remote pointing at a non fork.

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

1. `bash tools/port-compile.sh` and confirm it still says 1451.
2. `python tools/port-show.py` and work the thin spread. 121 files hold two or fewer errors and
   are mostly one lookup each.
3. Re-measure every pass. If a number moves a lot, check for parse errors before believing it.
4. Tool materials and models last.
5. At 0: build, run `python ../../tools/check-mixin-targets.py` if any mixins exist, then deploy
   alongside Mutil Refreshed and launch. Mutil has never been exercised by a real consumer, so
   that launch tests both at once.
