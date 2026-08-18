# Changelog

Tetra Refreshed is the Minecraft 26.1.2 NeoForge port of [Tetra](https://github.com/mickelus/tetra)
by Mikael Eriksson Vikner. Only the port is recorded here. Upstream's own history is in its
repository, and the mod itself is his work.

## 6.13.0, for 26.1.2

The first build that runs. Ported from PR 931, "Port to 1.21.1 neoforge" by evelant, which itself
sits on upstream 1.20.

**This is a test build and it is early.** It loads beside 150 other mods, creates and runs a world,
renders its items and opens its screens. What it has not had is play testing. No crafting flow, no
ability and no perk has been exercised beyond opening a screen, so expect to find things. Read the
known issues at the bottom before reporting.

Requires **Mutil Refreshed 7.0.0-pre.0** or later.

### Art of Forging ships inside Tetra too

**Art of Forging arrives with Tetra now**, the same way Secrets of Forging does, which is the second
half of what Ace asked for. **Remove any separate `art_of_forging` jar**, since two copies both
register `tetra:modular_artifact` and fail to load.

* It stays its own mod, by AceTheEldritchKing and MindFaer. The mod list shows it as its own entry,
  loaded from inside Tetra's jar.
* **The modular artifact is findable.** It was in no creative tab at all, not Tetra's and not its
  own mod's, which lists thirty ingredients and not the item they are for. The only way to one was a
  command. It is in Tetra's tab now, built with a casing and an internal so it works, and at the
  front of Art of Forging's own tab.
* **It is ninth in the holosphere**, after the single headed tool.
* It brings its own material category, eight reagents, which is what the artifact's internal slot
  takes.

**Three jars sharing one namespace stopped fighting.** Sixteen data files are claimed by more than
one of Tetra, Secrets of Forging and Art of Forging. Eleven of them had every copy asking to replace
rather than merge, so whichever loaded last silently discarded the others and took their content
with it. That included the sword, single and double socket modules, where all three mods add
variants and only one survived. They merge now.

**This has not been played.** Art of Forging loads and its data parses, and nothing in it has been
crafted or held.

### Materials from other mods

**37 new materials**, so tools can be built from what the rest of the pack already produces. Each is
one data file. No schematic changed, because schematics name a material directory and pick up
whatever is in it.

* **Regions Unexplored, 23 woods.** Alpha, baobab, blackwood, brimwood, cobalt, cypress, dead,
  eucalyptus, joshua, kapok, larch, magnolia, maple, palm, pine, redwood, socotra, willow, wisteria
  and the four bioshrooms. They share oak's stats deliberately, since those woods are not
  mechanically different from each other, and differ by colour, which is what does distinguish them.
  The 16 painted planks are left out as dye variants of one wood rather than woods of their own.
* **Applied Energistics 2, 4 materials.** Certus quartz and its charged form, fluix, and sky stone.
  The crystals carry far more magic capacity than a vanilla gem of the same durability, which is the
  thing that characterises them.
* **Oritech, 10 materials.** Steel, nickel, electrum, platinum, biosteel, energite, adamant,
  duratium, prometheum and fluxite. Their tiers follow Oritech's own recipes rather than a guess:
  steel is iron and coal, adamant is nickel and diamond, duratium is platinum and netherite, and
  prometheum comes out of the atomic forge, so it sits at the end.

Every tint is the average of that mod's own texture, calibrated against the ratio Tetra's ten
vanilla woods already use, rather than a colour picked by eye.

**None of this has been crafted.** The files load and parse, and the materials appear where they
should, but no tool has been built from one.

### Fixed

* **Mangrove was tinted cherry pink.** Its material file carried a verbatim copy of cherry's two hex
  values, so mangrove parts rendered pale pink rather than the dark red brown the planks actually
  are. Measured from the texture instead. Cherry itself was correct and is unchanged.
* **Nether star sockets had no name**, so the workbench and holosphere showed a raw translation key
  where every other material shows a name.

`tools/check-material-tints.py` is the check that would have caught the first one, and now does.

### The port

1941 compile errors down to zero, then nine further failures that only appeared on launch. The
detail is in `PORT-STATUS.md`. The parts worth knowing as a player:

* **Item rendering was rebuilt.** NeoForge's model geometry package is gone, so the custom loader
  that draws a modular item as a stack of module layers was rewritten onto the new item model
  system. Six classes collapsed into one.
* **Screens, renderers and particles** moved onto the extract and submit pipeline.
* **Inventories** moved onto the new item transfer api.
* **Blocks and items** carry their registry id on their properties now, which touched every one of
  the 49 registrations.

### Fixed during the port

* **Module attributes were being dropped in silence.** Attribute ids lost their `generic.` prefix in
  1.21, and the four affected ids resolved to nothing and were discarded without a word. Every
  modular item was contributing no attack damage, attack speed, armor or armor toughness from its
  modules. Found by reading the data, not by playing, so it is worth checking your numbers.
* **Modular items drew nothing.** Module tints are written without an alpha channel, and Tetra has
  always read an alpha of zero as opaque. The quad transformer that did that fix was one of the
  classes the model rebuild replaced, so every module whose tint omits the byte drew nothing. An
  obsidian hammer showed its heads and not its handle.
* **A held item floated about three blocks from the hand.** A display transform's translation is
  written in sixteenths of a block, and Tetra's own reader was passing the raw number through.
* **Items showed the missing texture.** An item's model is looked up under `assets/tetra/items` now.
  Seventy eight of them had none.
* **Block items showed `item.tetra.something`** rather than their name, because the block name
  prefix is a property now rather than something `BlockItem` decides for itself.
* **Opening the workbench crashed, then disconnected.** Two separate causes, both from the inventory
  move: the handler was truncating its own backing list, and the menus needed a modifiable view that
  the new adapter does not provide.
* **Recipes, loot tables and loot modifiers** were on formats that no longer parse. Thirty data
  files.
* **Curios was touched without a guard**, which crashed on the first player tick on any pack without
  it.
* **Shield modules drew the wrong texture.** 26.1 splits the item textures onto their own atlas, and
  the shield renderer was still asking the block atlas for them. Every other renderer in the mod
  draws block textures, so the shield was the only one affected.
* **The chthonic extractor showed `item.tetra.chthonic_extractor`.** It builds its own block items
  rather than going through the shared helper, so it never asked for the block name prefix.
* **The advancement tabs had no background.** A background is named as a plain texture id now and
  the game appends `textures/` and `.png` itself, so the old full paths resolved to nothing.
* **Long Gone was granted on world join** rather than on finding ancient ruins. A location predicate
  names `structures` now, holding any number of them, where it used to name a single `structure`.
  The old field was simply ignored, which left the predicate with nothing to check and therefore
  matching everywhere. Every other advancement was audited for the same shape and none of them fire
  early.

### Added

* **Secrets of Forging: Revelations ships inside Tetra now.** The polearm, its heads, handles and
  bindings, the sword, bow and socket modules and the three effects that drive them arrive with
  Tetra rather than as a mod you install separately. **Remove any separate
  `secrets_of_forging_revelations` jar**, since the bundled copy supersedes it and two copies would
  register `tetra:modular_polearm` twice and fail to load.
  * It stays its own mod, by AceTheEldritchKing, bundled at his request rather than absorbed. The
    mod list shows it as its own entry, loaded from inside Tetra's jar.
  * The polearm is in Tetra's own creative tab and is the fourth entry in the holosphere, between
    the double headed tool and the bow. It used to sit in vanilla's Combat tab with nothing listing
    it, which is why most players never found it.
  * Freezing, infernal and eternal blizzard now show a bar in the holosphere as well as the
    workbench.

* **Palette foundation.** A material may carry a colour palette, and a module may offer greyscale
  artwork. Where both exist the layer uses a sprite the atlas recoloured for that material, so a
  material can define its own look without artwork. Copper and iron hammer heads use it and render
  correctly. Every other material is untouched. `DEV.md` explains the intent.
* **Item types can be datapacks.** A handheld item can now be defined by an archetype file rather
  than a java class, declaring its module slots, their layout, honing, synergies and hit damage.
  The mechanism existed and had never been usable, because nothing could express enough to replace
  a class and no archetype had ever been written. `DEV.md` has the format.
* **Recipe viewer integration, for both EMI and JEI.** Tetra's materials are browsable, showing
  what counts as each material and what it contributes, in the same words the holosphere uses.
  Asking what an item is used for finds its material. Both are optional and a pack with neither is
  unaffected. EMI support needs EMI Refreshed, since upstream EMI stops at 1.21.1.
* **`DEV.md`**, a developer guide, and **`CURRENTPLANS.md`**, where the project goes after the port.

### Known issues

* **Villager trades are gone.** The api they used no longer exists and they need readding as data.
  Tetra currently sells nothing.
* **The interactive block overlay does not draw.** The salvage and interaction hints on block faces
  render nothing. It needs a world space drawing path that the gui toolkit does not have.
* **Inventories saved before this port will not load.** The workbench, rack and forged container
  changed storage format. Anything else is unaffected.
* **Modular items cannot be enchanted from a book at an anvil.** The hook that expressed the
  restriction is gone, so the restriction is simply absent.
* **One texture is missing**, the stonecutter sword blade held variant. The file has been zero bytes
  since an upstream commit, so it predates this port.
* **Transfer units and core extractors recompute slightly more often** than they used to, because
  the neighbour update no longer says where it came from.
* **A material outside vanilla's set can no longer be named in armor material data.**

### Reporting

Say what you did, what happened, and attach `logs/latest.log`. A crash report alone usually shows
only the launcher wrapper. If it failed to load, `logs/debug.log` has the real stack.
