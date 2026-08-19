# Current plans

Where Tetra Refreshed is going after the port. The port itself is in `PORT-STATUS.md`, and how to
build and run is in `DEV.md`.

This follows the public roadmap, three phases, with the detail a session needs to actually do the
work. Tetra and Mutil are Mikael Eriksson Vikner's. This project is a port and an extension of them
and claims neither.

## The aim

Make Tetra cheap to extend. Two things cost far more than they should today:

* **A material with a new look needs artwork for every module.** That is why 270 module textures
  ship for 107 materials.
* **Compatibility with another mod's tools is written by hand, every time.** That makes compat a
  permanent backlog rather than something that works by default.

Phase 1 exists to fix both. Phase 3 turns out to serve the same end, which is the one place the
public roadmap understates itself. See the note there.

# Phase 1, core systems

Developer friendly systems first, then a stable api on top of them. That order matters and is the
reason the api is last inside this phase rather than first: an api is a promise about shapes, and
the work ahead of it still changes what a material and an effect are.

## Done, the palette mechanism

A material may carry a `palette`, and a module may offer artwork under the `greyscale` texture
class. Where both exist the layer uses a sprite the atlas recoloured for that material, so a
material can define its own look without artwork of its own. A palette remaps the whole greyscale
ramp where a tint could only multiply one colour over it.

The mechanism is vanilla's `paletted_permutations`, the same one armour trims use, so the results
are ordinary atlas sprites with no shader or dynamic texture involved. The ramp is vanilla's own
eight greys, so artwork drawn for either fits both.

`tools/make-palettes.py` converts a module family in one command: it writes the key ramp, a palette
strip per material derived from the tint that material already carried, greyscale artwork quantised
from an existing variant, and the atlas source. `tools/palette.py` reads and writes the PNGs without
an image library, which was necessary because the artwork is four bit indexed PNGs.

Copper and iron carry palettes and the metal hammer variants offer greyscale. Netherite offers
greyscale and has no palette, so it stays on its own artwork, which is the fallback working.

**Checked by eye and correct.** The recoloured hammer heads read as normal tools, confirmed in game
on 2026-08-18. The mechanism is proven, not just stitching without error.

**The mechanism is done. The migration is one percent done.** Two materials of a hundred and seven
carry a palette, and two module textures of two hundred and seventy are greyscale. None of the
promised saving exists until that changes.

**Known constraint.** The atlas builds every texture and material combination, so palettes want
scoping to one material category per greyscale texture. All 107 materials against all 270 textures
would be about twenty nine thousand sprites, which is not viable. Worst case per category is wood at
thirty three.

This constraint is on palettes alone. A material that reuses an existing texture class costs one
json file and no sprites at all, which is how the 37 modded materials were added without touching
the atlas.

## In flight, generated materials

`MaterialGenerator` derives a material from any tool shaped item that no authored material covers.
`REPAIRABLE` gives the material item, `TOOL` gives mining speed and level, `MAX_DAMAGE` gives
durability and `ENCHANTABLE` gives magic capacity. Authored data always wins, because an item any
material already claims is skipped.

It reads the components rather than the `ToolMaterial` that usually writes them, because a
`ToolMaterial` is applied to an item's properties and not kept. Reading the result also covers tools
assembled some other way, which is most of the interesting cases.

**State: settled, and the answer is that this design cannot work. The source is deleted.**

Question two was the decisive one and the answer is no. `ImprovementStore.processData` expands
material improvements while the store parses, and `SchematicRegistry` expands material outcomes from
a listener on the schematic store, which also runs during the reload. Both read materials during the
reload. The generator injects after it, on the components bound event, because every stat it reads
is a component and components are unbound for the whole of a reload. Nothing that consumes materials
would ever see a generated one.

There is a second fault the questions did not anticipate. Every consumer finds materials by path
prefix over a category directory, `tetra:metal/` and the eleven others, which is 430 references
across the three projects. The generator keyed its output `tetra:generated/<namespace>/<path>`, and
nothing asks for `tetra:generated/`. So even with the timing fixed, a generated material would be
unreachable by every schematic and every improvement in the game.

**What a working version needs.** Keying is one line: put them under the category they claim, so
`metal/generated_<namespace>_<path>` rather than `generated/...`. Timing is the real cost. The only
point where components are bound and materials have not yet been consumed does not exist inside one
reload, so a working version has to inject on the event and then re-expand what depends on
materials, re-parsing `improvementData` from its raw data and re-firing the schematic listener. That
is a second full pass over 212 improvements and 547 schematics on every world load, on top of a
datapack load already near seven seconds.

That is a real feature with a real cost, and it wants deciding on its own rather than being carried
as a parked file that reads like unfinished work. The goal it serves, compatibility that does not
need a material written by hand for every modded tool, is still worth having. The approach has to
change: generation belongs where the data is read, not injected after everything has finished
reading it.

**What already went wrong twice**, worth knowing before picking it up: the generator first ran
inside `processData`, during the datapack reload. Every stat it reads is a component, and components
are unbound for the whole of a reload. `ItemStack` construction threw, and so did `Item.components()`.
That is why it moved onto the event. See the trap in `PORT-STATUS.md` section 8.

## The rest of phase 1, in order

1. **Roll the palettes out.** Convert module families and give materials palettes, category by
   category, checking each by eye. This is where the payoff is and the tool already exists. This is
   also the same work as phase 3, see the note there.
2. **Finish or revert the generator.** It wants the palette rollout ahead of it, because a generated
   material has no artist and lands on whatever artwork its category offers.
3. **Widen the data driven effect system.** `item_effects` already has outcomes, number providers,
   vector providers, entity providers and conditions resolved from json. Widen it until a new effect
   can be declared entirely in data, and port the built in effects onto it as they are touched. Do
   not attempt this as a sweep, ninety five effects is not a refactor anybody finishes.
4. **Config and hooks.** `ConfigHandler` exists and covers the mod's own options. What does not
   exist is a way for another mod to hook Tetra's behaviour without reaching into internals. Decide
   what is genuinely a hook and what is better expressed as data before writing any of it, because
   every hook is a promise the api then has to keep.
5. **An api package.** Move what an addon legitimately needs, effect registration, material lookup
   and module queries, into `se.mickelus.tetra.api`, and treat the rest as internal. Last on
   purpose, per the note at the top of this phase.
6. **Json schema files.** The `schemas` directory exists at the repository root and is empty.
   Filling it gives editor completion to every datapack author. It locks nothing, so it can slot in
   at any point.

# Phase 2, content integration

Fold Art of Forging and its companion addon into Tetra as base content rather than shipping them
alongside.

| Mod | Terms | State |
|---|---|---|
| <https://github.com/AceTheEldritchKing/Secrets-Of-Forging-Revelations> | MIT with restrictions, in its README | **bundled with jarJar, loads clean** |
| <https://github.com/AceTheEldritchKing/art_of_forging> | none in the repo, MIT in gradle.properties | **ported and bundled with jarJar, never run** |

**Ace granted permission on 2026-08-18, on Discord**, for both his mods, with two conditions. He
asked that they be **separate projects included in the mod** rather than flattened into this source
tree, naming how Create bundles Flywheel as a jarjar file, and that he be **credited as a
contributor** as well as in the README.

**Secrets of Forging is bundled.** It stays its own repository and its own mod id, is published to
mavenLocal, and Tetra embeds it with jarJar so NeoForge loads it from inside this jar. Nothing of it
lives in this source tree. Everything it adds to Tetra's screens is reached from its side: the
polearm joins Tetra's creative tab by matching the tab id, and its holosphere entry and effect stat
bars ship as data under `assets/tetra`, which is where Tetra's stores look. Its `PORT-STATUS.md`
has the detail.

Publish the addon to mavenLocal before building here, the same as mutil:

```bash
cd "../Secrets-Of-Forging-Revelations" && ./gradlew.bat publishToMavenLocal
```

A flat merge of it was tried first and is parked on the `flat-merge-sofr` branch. It works, and it
is the wrong shape, so take integration decisions from it rather than files.

**Art of Forging is bundled too**, on the same terms and by the same mechanism, which settles the
compatibility content Secrets of Forging carries for it. That namespace exists now.

**It is an addon for Secrets of Forging rather than for this mod**, so the chain is
`tetra <- secrets_of_forging_revelations <- art_of_forging` and it is declared that way. Its
schematics target polearm slots, which only exist because Secrets of Forging defines them.

It is ported and it builds, and **it has never been run**. Its own `PORT-STATUS.md` records what
changed and what is known to be wrong, including two things that predate the port: its loot
modifiers read a field name none of their data files write, and its creative tab was registered by
nothing.

**Three jars write into `data/tetra` now.** That is the whole reason the arrangement works, and the
one way it goes wrong: when two jars claim a path, load order decides. A file that sets
`"replace": false` merges instead, and `variants` and `outcomes` concatenate, so an addon extending
Tetra's own file loses nothing. Sixteen files are shared and eleven of them were replacing rather
than merging, which meant the sword, single and double socket modules each kept one mod's variants
and threw away two. `tools/check-bundle-collisions.py` reads the built jar and reports any path more
than one jar claims, separating the deliberate merges from the ones load order decides.

**Art of Forging has no licence at all.** No file, nothing in its README, and GitHub reports none,
so no permission to reuse is granted by default. Ask before touching it.

The perk restriction in Tetra's terms applies to anything merged that touches perks.

# Phase 3, visual polish

Retexture the mod's own items:

Geode, Pristine Lapis Lazuli, Pristine Emerald, Pristine Diamond, Pristine Amethyst, Forged Blocks,
Holosphere, Bolt, Structure Beam, Flex Mesh, Metal Scrap, Thermal Cell, Seeping Bedrock, Rope.

**This is not only polish, and it should not be scheduled as if it were.** Every one of those is
artwork somebody has to draw. Drawing it in greyscale against the palette ramp costs the same as
drawing it in colour, and the result then works for every material that carries a palette rather
than only the one it was drawn for.

So phase 3 done greyscale first is phase 1's migration getting done for free. Done in colour it is a
second pile of artwork the palette system cannot use, and the rollout still has to happen afterwards.
Whoever picks up the retexturing should read the palette section above before starting, and `DEV.md`
explains the ramp.

The five pristine gems are the clearest case: one shape in five colours is exactly what one
greyscale texture and five palettes expresses.

# Missing features, not modernisation

Things the mod used to do and no longer does. Worth separating from everything above, because a
player notices these and nobody notices an api.

* **Villager trades.** Removed during the port because the events they hung off no longer exist.
  Tetra sells nothing. Readding them is data rather than code, and `PORT-STATUS.md` section 7 has
  the format and names the commit the old table lives in. The scroll trades are the fiddly ones,
  because their result is a stack carrying scroll components rather than a plain item.
* **The interactive block overlay.** The salvage and interaction hints on block faces draw nothing.
  It needs a world space drawing path that mutil's gui toolkit does not have, which makes it the
  largest of these by some distance.
* **Book enchanting at an anvil.** Modular items used to be excluded and now are not, because the
  hook that expressed it is gone. Whether to reimplement or accept the new behaviour is a design
  call, not a port one.
* **The stonecutter blade held texture** is a zero byte file upstream. One texture, and now that the
  port is finished, rule 5 no longer blocks fixing it. Phase 3 work.

# Debts and landmines

Things that will bite somebody later if nobody writes them down.

* **Datagen would break the item models.** `TetraBlockStateProvider` writes `models/item` and never
  writes `items`, but an item's model is looked up under `items` now. The thirty eight generated
  entries beside it were written by hand, so running `runData` regenerates the block models and
  leaves those stale. Teach the provider to emit both before anybody runs it.
* **The dedicated server has never had a client connect to it.** It starts clean, which is what got
  checked. Everything past that is unproven.
* **The `@OnlyIn` warning is narrower than it reads.** All 122 uses sit on members rather than on
  types, and a method body is resolved when it is called rather than when its class loads, so an
  uncalled client only method does not stop a server loading the class. What does break at load
  time is a static field of a client type, and a scan found ten, all of them in renderers or the
  datagen provider, none on a path a server takes. The residual risk is a server path calling one
  of those methods, which is a question about call sites rather than about class loading.
* **Silent drops are the failure mode of this codebase.** The attribute prefix bug discarded every
  module attribute without a word. The Long Gone advancement then did it again, in vanilla data
  rather than Tetra's: a location predicate renamed `structure` to `structures`, the old field was
  ignored rather than rejected, and a predicate with nothing left to check matches everywhere. Both
  were found by reading data rather than by playing.

  So the audit is wider than the deserializers. Tetra's own deserializers resolve names and return
  null on a miss, and should log a warning at each one. Vanilla's codecs ignore unknown fields
  entirely, which means every renamed field in ported data fails open and looks like nothing
  happened.

  **The vanilla half is now checked and clean.** `tools/check-data-fields.py` resolves each object
  to its own codec and reports keys nothing reads, across all 163 advancement, loot table, loot
  modifier and recipe files. It is verified against the Long Gone bug itself. What it found was two
  authoring labels and no defects. Most loot and recipe fields turn out to be required rather than
  optional, so they fail loudly, which is why the damage concentrated in an optional predicate
  field. **Tetra's own deserializers are still unchecked**, and no tool can check them, because
  returning null on a miss is their own code rather than a codec.
* **Upstream issues are now in scope.** Rule 5 held them until the port was done. It is done. See
  <https://github.com/mickelus/tetra/issues>.
* **`tetra:draw_damage` does not exist.** Two improvement files reference it as an attribute,
  `shared/quality` and `shared/destabilized/ravenous`, and nothing has ever registered it, in any
  commit. So those improvements silently contribute nothing on that line. It is upstream's own
  dangling reference rather than something the port broke, which is why it is here rather than
  fixed. Either register it or drop the references.
* **Mutil has no developer documentation.** It is the shared library and the generic half of the api
  work belongs in it, so it will need a `DEV.md` of its own.

## What a modernisation pass should and should not touch

296 dead imports have been removed across both mods, 21 of them exact duplicates left by collapsing
the old interaction result types onto one. That shrinks the diff against upstream, because the
residue was the port's rather than upstream's.

**The rest should not be swept.** Reviewing against upstream is the whole value of staying a fork,
and rewriting 798 files into newer idioms destroys it for no behaviour gain. Modernise a file when
there is already a reason to open it. What is waiting there:

* **34 `Collectors.toList()` calls.** Each needs reading rather than replacing, because `.toList()`
  returns an immutable list and some of these results are mutated afterwards.
* **`LazyOptional`** is 79 lines reimplementing a Forge class that no longer exists, for two call
  sites. Both are behaviour bearing, so it is a real refactor rather than a tidy.
* **57 todo markers**, some of which name versions long past, such as one addressed to 1.12.
* **Three anonymous classes** that predate lambdas being available for their interfaces.

# What gates all of it

The mod has had almost no play testing. It loads, runs a world, renders and opens its screens, and
that is the whole of what is proven. Adding architecture on top of unplayed code produces a large
pile of things that each half work. Test feedback should interrupt anything on this list.

`CHANGELOG.md` lists the known gaps a tester will hit.

**Distribution: asked and waiting.** Mikael was emailed on 2026-08-18 for permission to hand
testers a jar, since Tetra's terms forbid redistributing it as compiled code. Do not raise this
again until there is a reply. Mutil is MIT and has no such problem.
