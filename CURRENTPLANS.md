# Current plans

Where Tetra Refreshed is going after the port. The port itself is in `PORT-STATUS.md`, and how to
build and run is in `DEV.md`.

## The aim

Make Tetra cheap to extend. Two things cost far more than they should today:

* **A material with a new look needs artwork for every module.** That is why 268 module textures
  ship for 68 materials.
* **Compatibility with another mod's tools is written by hand, every time.** That makes compat a
  permanent backlog rather than something that works by default.

Everything below serves one of those, or clears the way for them.

## Done

### Palette driven module textures

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

**Known constraint.** The atlas builds every texture and material combination, so palettes want
scoping to one material category per greyscale texture. All 68 materials against all 268 textures
would be about eighteen thousand sprites, which is not viable. Worst case per category is fabric at
sixteen.

## In flight, unfinished

### Generated materials

`MaterialGenerator` derives a material from any tool shaped item that no authored material covers.
`REPAIRABLE` gives the material item, `TOOL` gives mining speed and level, `MAX_DAMAGE` gives
durability and `ENCHANTABLE` gives magic capacity. Authored data always wins, because an item any
material already claims is skipped.

It reads the components rather than the `ToolMaterial` that usually writes them, because a
`ToolMaterial` is applied to an item's properties and not kept. Reading the result also covers tools
assembled some other way, which is most of the interesting cases.

This depends on the palette work. Before it a generated material had no artist, so sixty eight
generated materials would all have rendered as the fallback texture.

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

## Next, in order

1. **Finish or revert the generator.** It is the largest remaining win and it is nearly done. Half
   finished uncommitted work is the worst state to leave it in.
2. **Widen the data driven effect system.** `item_effects` already has outcomes, number providers,
   vector providers, entity providers and conditions resolved from json. Widen it until a new effect
   can be declared entirely in data, and port the built in effects onto it as they are touched. Do
   not attempt this as a sweep, ninety five effects is not a refactor anybody finishes.
3. **An api package.** Move what an addon legitimately needs, effect registration, material lookup
   and module queries, into `se.mickelus.tetra.api`, and treat the rest as internal. **This goes
   last on purpose.** An api is a promise about shapes, and both the generator and the effect work
   change what a material and an effect are. Publishing one first means either breaking it
   immediately or designing around shapes that do not exist yet.
4. **Json schema files.** The `schemas` directory exists at the repository root and is empty.
   Filling it gives editor completion to every datapack author. It locks nothing, so it can slot in
   at any point.

## What gates all of it

The mod has had almost no play testing. It loads, runs a world, renders and opens its screens, and
that is the whole of what is proven. Adding architecture on top of unplayed code produces a large
pile of things that each half work. Test feedback should interrupt anything on this list.

`CHANGELOG.md` lists the eight known gaps a tester will hit, of which villager trades selling nothing
and the block overlay not drawing are the two anybody will notice first.

**Distribution is unresolved.** Tetra's terms forbid redistributing it as compiled code, which is
what handing a tester a jar is. Forking and modifying is permitted and that is what this repository
is. Until that question is answered with Mikael, the feedback loop the whole list depends on does
not really exist. Mutil is MIT and has no such problem.
