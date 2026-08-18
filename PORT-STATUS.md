# Tetra Refreshed, port status

Port of Tetra from 1.21.1 NeoForge to 26.1.2 NeoForge. In progress, does not compile yet.

## Base

Upstream Tetra's newest branch is `1.20`. The base used here is instead PR 931,
"Port to 1.21.1 neoforge" by evelant, head `evelant/tetra` branch `1.21` at `8aa22195`. That
PR is open and unmerged, 884 files changed. Starting from it turns a 1.20 to 26.1.2 jump into
a 1.21.1 to 26.1.2 one, which is the same jump Mutil Refreshed just made.

792 Java files.

## Progress

| Stage | Unique errors |
|---|---|
| First uncapped compile against 26.1.2 | 1941 |
| After ResourceLocation, GuiGraphics, isClientSide, critereon, FastColor | 1601 |
| After package moves and merged result types | 1601 |
| After getCommandSenderWorld, sidedSuccess, hasShiftDown, getNormal | 1475 |

`javac` caps error output. Early numbers were capped at 100 and then 2000, so they understated
the real count. 1941 is the first honest figure. Anything lower than that is real progress.

## Done

Toolchain on Duty's known good set. Gradle 9.6.1, ModDevGradle 2.0.144, Java 25, NeoForge
26.1.2.95, Parchment 1.21.11.

`local_maven` publishing guarded, the same trap Mutil had. Unset off upstream's Jenkins, so the
url became `file://null` and killed the configuration phase before any compile.

Mutil Refreshed is published to mavenLocal and resolved as `26.1.2-7.0.0-pre.0`. Tetra depends
on mutil by maven coordinate, so mutil had to be a real artifact before Tetra could compile at
all.

Renames applied in bulk, each confirmed against the jar with javap rather than guessed:
`ResourceLocation` to `Identifier` (485), `isClientSide` field to accessor (140), `GuiGraphics`
to `GuiGraphicsExtractor` (68), `ItemInteractionResult` and `InteractionResultHolder` folded
into `InteractionResult` (105), `advancements.critereon` to `criterion` (25), `UseAnim` to
`ItemUseAnimation` (22), `getCommandSenderWorld` to `level()` (46), `sidedSuccess` to a constant
(30), `hasShiftDown` from `Screen` to `Minecraft` (21), `getNormal` to `getUnitVec3i` (18),
`DirectionProperty` to `EnumProperty<Direction>` (18), `FastColor` to `ARGB` (18), plus package
moves for `AbstractArrow`, `Material` and `ItemTransforms`.

## Left, 1475 unique

| Count | Share | Group |
|---|---|---|
| 871 | 59% | uncategorised, mostly thin spread of individual API changes |
| 251 | 17% | signature changes surfacing as failed overrides |
| 73 | 5% | `CompoundTag` getters return `Optional` now |
| 71 | 5% | `InteractionResult` is an interface, not an enum |
| 60 | 4% | fields became accessors |
| 48 | 3% | model system |
| 47 | 3% | tool material system |
| 30 | 2% | `BlockEntity` load and save signatures |
| 24 | 2% | GUI and render layers |

## Why this is not a grind

Two of those groups are not renames and cannot be batched.

**Tool materials.** `Tier` does not exist in 26.1.2. The tool material system was replaced. Tetra
is a modular tool mod whose entire premise is composing tool behaviour, so this is not a lookup,
it is deciding how Tetra's tiers map onto the new model.

**Models.** `BakedModel` does not exist either, and
`net.neoforged.neoforge.client.model.geometry` is gone. Tetra ships a custom model loader for
modular item rendering. That is a rewrite against a model pipeline that now resolves and bakes
differently.

`InteractionResult` becoming an interface breaks every `switch` over it, because you cannot
switch over interface constants the way you can over enum values. Those need reading, not
substituting.

## Repo blocker

`EternalCoder454/Tetra-Refreshed` exists but is **not a fork**. The API reports
`fork: false, parent: null`.

Tetra's licence permits forking and modifying. It prohibits redistributing the project in its
entirety as source or compiled code. A standalone public repository holding a full port is
exactly that. Nothing here should be pushed to that repo until it is a real GitHub fork of
`mickelus/tetra`, which means deleting it and using the fork button, since a plain repo cannot
be converted.

Local work is fine. Forking and modifying is permitted, and none of this has been published.

## Next

Work the 871 uncategorised first, since they are thin and mechanical and will shrink the other
groups by cascade, exactly as `getCommandSenderWorld` did when it removed several hundred
downstream errors on its own. Leave tool materials and models until last, when the surrounding
code compiles and the shape of the problem is visible.
