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

### Added

* **Palette foundation.** A material may carry a colour palette, and a module may offer greyscale
  artwork. Where both exist the layer uses a sprite the atlas recoloured for that material, so a
  material can define its own look without artwork. Nothing uses it yet, and every existing material
  is untouched. `DEV.md` explains the intent.
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
