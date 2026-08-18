# Current plans

Where Tetra Refreshed is going after the port. The port itself is in `PORT-STATUS.md`, and how to
build and run is in `DEV.md`.

This follows the public roadmap, three phases, with the detail a session needs to actually do the
work. Tetra and Mutil are Mikael Eriksson Vikner's. This project is a port and an extension of them
and claims neither.

## The aim

Make Tetra cheap to extend. Two things cost far more than they should today:

* **A material with a new look needs artwork for every module.** That is why 270 module textures
  ship for 70 materials.
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

**Not yet checked by eye.** The atlas stitches with no missing texture reported and the sprite names
line up exactly, but nobody has looked at a copper hammer head to see whether it reads well.

**The mechanism is done. The migration is one percent done.** Two materials of seventy carry a
palette, and two module textures of two hundred and seventy are greyscale. None of the promised
saving exists until that changes.

**Known constraint.** The atlas builds every texture and material combination, so palettes want
scoping to one material category per greyscale texture. All 70 materials against all 270 textures
would be about nineteen thousand sprites, which is not viable. Worst case per category is fabric at
sixteen.

## In flight, generated materials

`MaterialGenerator` derives a material from any tool shaped item that no authored material covers.
`REPAIRABLE` gives the material item, `TOOL` gives mining speed and level, `MAX_DAMAGE` gives
durability and `ENCHANTABLE` gives magic capacity. Authored data always wins, because an item any
material already claims is skipped.

It reads the components rather than the `ToolMaterial` that usually writes them, because a
`ToolMaterial` is applied to an item's properties and not kept. Reading the result also covers tools
assembled some other way, which is most of the interesting cases.

**State: written, compiles, never successfully run, and taken back out of the branch.** It briefly
landed in commit ffbfb19 by accident, wired into DataManager, which would have run it unverified in
any build from that commit. The reverting commit says where the source is parked. Bring it back by
restoring three things: the generator class, the `OutcomeMaterial.of` factory, and the event hook in
`DataManager`.

**Two questions to settle before it can be called done:**

1. Does `DefaultDataComponentsBoundEvent` fire after the material store has parsed? If it fires
   before, the generator sees an empty map and claims items an authored material should have.
2. Does anything that depends on materials, `improvementData` in particular, ever see materials
   injected after the reload rather than during it?

If either answer is bad, reverting is the honest outcome. Nothing else references it.

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

| Mod | State |
|---|---|
| <https://github.com/AceTheEldritchKing/art_of_forging> | no licence file |
| <https://github.com/AceTheEldritchKing/Secrets-Of-Forging-Revelations> | no licence file |

Neither carries a licence, which means no permission to reuse is granted by default. Ask the author
before merging either. This is a different question from the one already put to Mikael and it is
still open.

Both are earlier era content and will need the same port this repository just did, so merge after
phase 1 rather than during. The perk restriction in Tetra's terms applies to anything merged that
touches perks.

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
  checked. Everything past that is unproven, and NeoForge warns that both mods still use `@OnlyIn`
  while it no longer strips anything, so more client classes may be reachable from server paths. The
  five found during the port were found one crash at a time.
* **Silent drops are the failure mode of this codebase.** The attribute prefix bug discarded every
  module attribute without a word, and was found by reading data rather than by playing. The same
  shape exists wherever a deserializer resolves a name and returns null on a miss. Worth an audit of
  the data deserializers, and worth logging a warning at each one.
* **Upstream issues are now in scope.** Rule 5 held them until the port was done. It is done. See
  <https://github.com/mickelus/tetra/issues>.
* **Mutil has no developer documentation.** It is the shared library and the generic half of the api
  work belongs in it, so it will need a `DEV.md` of its own.

# What gates all of it

The mod has had almost no play testing. It loads, runs a world, renders and opens its screens, and
that is the whole of what is proven. Adding architecture on top of unplayed code produces a large
pile of things that each half work. Test feedback should interrupt anything on this list.

`CHANGELOG.md` lists the known gaps a tester will hit.

**Distribution: asked and waiting.** Mikael was emailed on 2026-08-18 for permission to hand
testers a jar, since Tetra's terms forbid redistributing it as compiled code. Do not raise this
again until there is a reply. Mutil is MIT and has no such problem.
