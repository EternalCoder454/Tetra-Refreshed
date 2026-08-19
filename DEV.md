# Developer guide

Building, running, and the data formats. For the port itself see [PORT-STATUS.md](PORT-STATUS.md),
and for where the design is going see [CURRENTPLANS.md](CURRENTPLANS.md).

## Building

Gradle 9.6.1, ModDevGradle 2.0.144, Java 25, NeoForge 26.1.2.95, Parchment 1.21.11 with 2025.12.20
mappings. Mutil Refreshed has to be in mavenLocal first, because Tetra resolves it by coordinate:

```bash
cd "../Mutil Refreshed" && ./gradlew.bat publishToMavenLocal
cd "../Tetra Refreshed" && ./gradlew.bat build
```

## Running

```bash
bash tools/run.sh                 # build, deploy to the test pack, launch, report whether it loaded
./gradlew.bat runClient           # tetra and mutil alone, no other mods
./gradlew.bat runClient -PquickPlay=<world>
./gradlew.bat runServer           # dedicated server, exercises data loading without a gui
```

`run.sh` is the honest check. A compiling jar says nothing about whether the mod loads, and most of
the failures in this port surfaced only by launching. It also kills the stale `javaw` that keeps the
jar locked after a crash, which otherwise makes the copy fail silently.

`runServer` is the fastest loop for anything server shaped. It reaches `Done` in seconds and its log
is short enough to read whole.

## Checking

```bash
bash tools/port-compile.sh && bash tools/port-check.sh   # compile and count errors honestly
python tools/check-at.py                                 # every access transformer entry still resolves
python tools/check-data-fields.py                        # every data key is read by the codec that owns it
python tools/check-material-tints.py                     # every tint still matches the item it is made from
python tools/check-bundle-collisions.py                  # no two bundled jars claim the same path
python tools/check-bus-registrations.py                   # every bus registration names a class with a listener
python tools/check-model-types.py                         # every module model declares a type Tetra can read
python tools/check-material-items.py                      # every material names an item some installed mod has
python ../../tools/check-mixin-targets.py "Mickelus Mods/Tetra Refreshed"
python ../../tools/check-writing-rules.py <file>         # prose rules for the docs here
```

`port-check.sh` exists because a parse error aborts analysis and collapses the error count into
something that reads like near success. Run it before believing any number.

`check-at.py` matters because a stale access transformer entry is ignored rather than failing the
build, so the widening silently never happens and surfaces much later as a private access error
somewhere unrelated.

`check-material-items.py` matters because of what an unresolvable item used to do.
`SimpleItemPredicate` drops item ids it cannot resolve, and a predicate left with no items and no
tag filters by nothing, so it said yes to every item in the game. Art of Forging names three
materials from mods that need not be installed, and with none present one was a fibre with 6.5
hardness and one a metal with 8.5 hardness and a netherite tool level, both accepting anything that
fit in a slot. A holosphere made a claw, wood wrapped a handle, and hammer tiers came from nowhere.
The predicate matches nothing in that state now, so these are inert, and the checker reports them
because inert and mistyped look identical from the outside.

`check-model-types.py` matters because a model type outside the registry throws out of gson while a
module or improvement is being read, and the message names the type and nothing else, not the file.
Art of Forging shipped six such types across twelve files, and each one was a value that belonged in
a different field: a filter name or a model name sitting in `type`. Until mutil started dropping
files one at a time, any one of them emptied the whole store.

`check-bus-registrations.py` matters because Forge accepted `EVENT_BUS.register` for a class with no
`@SubscribeEvent` on it and quietly did nothing, while NeoForge throws during mod construction and
takes the whole mod down before it loads. Art of Forging registered a charged ability that way and
never had a listener on it, so the call had been dead since it was written and only became fatal on
the port. The message names one class, which reads as specific when the mistake is usually a pattern.

`check-bundle-collisions.py` matters because this jar carries two other mods inside it, and all
three write into `data/tetra` and `assets/tetra`, which is what makes the arrangement work at all.
When two of them claim one path, load order decides which a player gets, and no mod controls load
order. A data file that sets `"replace": false` merges into what is already there rather than
throwing it away, so the checker separates those from the ones that really are a race. It reads the
built jar, so build first.

`check-material-tints.py` matters because nothing validates a tint. A material carrying another
material's colours parses, loads and renders, it just renders wrong, and the only way to notice is to
look at the item and already know what it should look like. That is how mangrove shipped tinted
cherry pink: its file held a verbatim copy of cherry's two hex values. So the checker measures the
texture of the item the material is made from and reports how far the declared tint sits from it.

Its findings are candidates rather than verdicts, and the false positives are worth knowing. A white
tint is the idiom for letting module artwork through unchanged, so those are listed separately rather
than flagged. Gems and dyed wool are drawn brighter than the item they come from on purpose, so they
sit near the top of the list and belong there. Read a finding before changing it.

`check-data-fields.py` matters for the same reason one level up. A data file that fails to parse is
logged and dropped, which gets noticed. A field that was renamed is ignored, which does not: the
constraint it expressed disappears and whatever it guarded now always passes. That is how the Long
Gone advancement came to be granted on world join. It reports candidates rather than verdicts, so
read each one, and note that authoring labels show up here too.

## Looking things up

Never guess a signature. Ask the jar:

```bash
bash tools/jp.sh net.minecraft.world.item.ItemStack          # javap, whole compile classpath
bash tools/jp.sh net.minecraft.nbt.CompoundTag getInt        # filtered
bash tools/src.sh net/minecraft/world/InteractionResult      # decompiled source
```

`jp.sh` reads the classpath Gradle actually resolves, from `build/port/cp.txt`. Globbing the cache
for a jar by name has answered confidently and wrongly more than once.

## Editing sources

Every Java file here is CRLF. A multi line replacement written with `\n` silently matches nothing,
and a replace that matches nothing reports success, so the edit reads as applied and did nothing.
Use `tools/portedit.py`, which asserts each search string matches exactly once and preserves the
file's line endings.

## Reading and writing modular item data

A modular item keeps its modules, variants, improvements and honing state in the `CUSTOM_DATA`
component, reached through `ItemStackTagHelper`. Three methods, and which one you pick matters:

| Method | Use for | Cost |
|---|---|---|
| `readTag` | reading, and nothing else | free, returns the live tag |
| `getTag` | when the tag has to outlive the call, or a sub tag escapes | one deep copy |
| `mutate` | every write | one deep copy, written back atomically |

**`readTag` returns the component's own tag.** Mutating it writes straight through to a value that
is shared between stacks, so it would change other items. Every write goes through `mutate`, which
hands you a detached copy and puts it back when the lambda returns.

Reading used to cost two deep copies of an item's entire tag, because `getTag` copied what
`copyTag` had already copied, and every module lookup, improvement check and cache key went through
it. Pulling one string out of an item copied the whole thing twice. That is what `readTag` is for,
and it is why the access transformer widens `CustomData.tag`.

## Things that run every frame or every tick

Three rules the code already follows. Each was a measured cost rather than a preference, so undoing
one puts the cost back.

**Caches on a modular item are keyed by `getDataCacheKey`.** There are six: attributes, tool data,
effects, properties, enchantability and the tool component. The key is the item's module identifier,
so a cache holds while the modules do. **Anything that depends on the item's damage cannot go behind
that key**, because damage is not part of it. The tool component is the live example: it returns
nothing for a broken item, so broken is checked outside the cache. Cached inside, a broken tool would
have kept the mining rules it had when it was whole.

**The gui toolkit iterates with indexed loops, not streams.** Every element draws its children, so
`drawChildren`, `updateFocusState` and `getTooltipLines` run once per element per frame, and a stream
there allocates a pipeline, a spliterator and a lambda each time. The duplication between
`GuiElement` and the classes that override those methods is deliberate.

**Reading an item's tag is free, copying it is not.** See the table above. `readTag` for reads,
`mutate` for writes, `getTag` only when the tag has to outlive the call.

## The data formats

Tetra is data driven across these datapack directories under `data/tetra`:

| Directory | What lives there |
|---|---|
| `materials` | material stats, textures and tints |
| `modules` | module definitions, variants and models |
| `schematics` | what can be crafted onto what |
| `improvements` | honing, settling and enchantment mappings |
| `synergies` | bonuses for module or material combinations |
| `item_effects`, `modifier_effects`, `crafting_effects` | effect behaviour, as data |
| `tweaks`, `tiers`, `repairs`, `replacements`, `actions` | the rest of the crafting model |

Client side data lives under `assets/tetra`: `stat_bars`, `stat_indicators`, `stat_sorters`,
`holosphere_entries` and `tool_actions`.

### Better Combat

Better Combat reads `data/<namespace>/weapon_attributes/<item path>.json`, where the file path is
the item id, and the smallest useful file names a preset it already ships:

```json
{ "parent": "bettercombat:sword" }
```

That is the whole integration. Nothing sits between the two mods, there is no mixin, and an item
with no file is left exactly as it was, which is why vanilla bows still behave normally and why
Tetra's bow and crossbow are deliberately left out.

| item | preset | why |
|---|---|---|
| `tetra:modular_sword` | `bettercombat:sword` | one handed, three attacks |
| `tetra:modular_single` | `bettercombat:axe` | one handed with shorter reach, a hatchet or a pick |
| `tetra:modular_double` | `bettercombat:double_axe` | two handed, the two headed tools |
| `tetra:modular_polearm` | `bettercombat:pike` | Secrets of Forging ships this preset itself |

The shield and toolbelt are not weapons, the holosphere is a menu and the artifact is a curio, so
none of them get a file.

**One pattern per item, not per build.** A sword is a sword whether its blade is a machete or a
rapier, and this cannot tell them apart. Tetratic Combat could: it read the modules off the stack
and resolved a pattern per build, and its `data/tetratic/configs/tetra.json` is a map from module
variant to preset that is worth reading if this is ever taken further. It stops at 1.18, and its
approach needs a mixin into Better Combat, because the only registration Better Combat offers is one
pattern per item id rather than per stack.

**Both addons already ship presets for builds nothing can select yet.** Secrets of Forging has
`longsword` and `one_hand_katana`, Art of Forging has `crucible` and `rending_scissor`, all
under `data/bettercombat/weapon_attributes`, all written for a katana blade or a crucible blade
rather than for the item as a whole. They are inert until something resolves per build, and they are
the reason to do that properly rather than approximate it.

### When two materials claim the same item

A material may name items or claim a tag, and the two overlap on purpose. Oak claims
`minecraft:planks` so that any plank can be used, and every wood with a material of its own also
names its own planks. Wool does the same across fifteen colours. In this pack that is thirty two
plank items and fifteen wool items matched by two materials each.

**The one that names the item wins.** Naming an item is the more specific claim, so a maple plank
gives maple, and a plank no material names still falls back to oak. Within either group the last
match still wins, which is what lets an addon override.

This used to be whichever match came last out of the material store, which is a hash map, so the
answer was arbitrary rather than chosen. Adding twenty three woods turned a quirk into a coin flip.

### Adding a material

A material is one file under `data/tetra/materials/<category>/<name>.json`:

```json
{
    "key": "iron",
    "category": "metal",
    "primary": 5, "secondary": 3.8, "tertiary": 3,
    "durability": 250,
    "integrityCost": 2, "integrityGain": 5,
    "toolLevel": "minecraft:iron",
    "toolEfficiency": 6,
    "tints": { "glyph": "iron_glyph", "texture": "iron" },
    "textures": [ "metal", "default" ],
    "material": { "items": [ "minecraft:iron_ingot" ] }
}
```

`textures` names the artwork classes this material prefers, in order. A module offers a set of
classes, and the first match wins, falling back to the module's first available class. So a material
that resembles something already drawn costs exactly this one file.

A material that needs a look nothing else has needs artwork for every module, which is why 270
module textures ship. The palette below is the way out of that.

**A new file is enough. Nothing has to be wired to it.** Schematics name a material directory rather
than a list of files, `tetra:metal/` and `tetra:wood/` and so on, and `SchematicRegistry` expands any
reference ending in a slash across everything in that directory. So a file dropped into
`materials/metal` is offered by all 48 schematics that ask for metal, with no schematic edit.

**The tint is a colour multiply at draw time, not a generated sprite**, so tints are free no matter
how many materials carry them. Only `palette` costs atlas space. A tint may name an `ItemColors`
entry or give raw hex, and raw hex is the right choice for a material Tetra does not know about.

Deriving a tint from the mod's own texture beats picking one by eye. Averaging the ten vanilla plank
textures against the tints Tetra declares for them gives a median ratio of about 1.1 per channel, and
Tetra's glyph tint sits at about 0.85 of its texture tint, so a modded material lands in the same
relationship to its art as a built in one.

`tools/check-material-tints.py <name>` does that measurement and prints what the tint would be, for
any material in any mod the test pack has installed. With no argument it audits every material.

### Adding a material with a palette

A material may instead carry a colour palette:

```json
"palette": "tetra:colormap/material/mithril"
```

A module that offers the `greyscale` texture class draws that artwork once, in greyscale. The atlas
builds one recoloured sprite per palette carrying material from it, and the model picks the sprite
named for the material. A palette remaps the whole greyscale ramp rather than multiplying a single
colour over it, so it can define a look rather than only shift one.

A material with a palette and a module with greyscale artwork therefore cost one json file and no
artwork at all. Materials without a palette are untouched and keep the texture and tint they always
used, so this is additive.

The palette itself is a texture, the same shape as vanilla's armour trim palettes: a strip of
colours matching the key ramp, position for position. Restrict palettes to materials of one category
per greyscale texture, because the atlas builds every combination and the matrix grows quickly.

### Adding an item type

Every modular item used to be a java class. `tetra:dynamic_handheld` is a handheld item that reads
its shape from an archetype instead, one file under `data/tetra/archetypes/<key>.json`:

```json
{
    "honeable": true,
    "honeBase": 120,
    "honeIntegrityMultiplier": 60,
    "synergyPrefix": "single/",
    "entityHitDamage": 1,
    "slots": [
        { "key": "single/head", "major": true, "required": true, "x": 1, "y": -3 },
        { "key": "single/handle", "major": true, "required": true, "x": -11, "y": 21 },
        { "key": "single/binding", "major": false, "required": false, "x": -14, "y": 0 }
    ]
}
```

The file name is the key. A major slot holds a module that can carry improvements, a minor slot one
that cannot, and `x` and `y` place the slot in the workbench relative to the item. `synergyPrefix`
names a directory under `data/tetra/synergies` whose synergies apply to this type. Anything omitted
falls back, so the shortest useful archetype is a `slots` array on its own.

`data/tetra/archetypes/reference.json` ships as a working example and nothing grants it in game. An
archetype reaches a player two ways, and both name the key as a plain string:

* a replacement, under `data/tetra/replacements`, with `"item": "tetra:dynamic_handheld"` and
  `"archetype": "<key>"`, which converts a matching item on pickup
* a holosphere entry, under `assets/tetra/holosphere_entries`, with the same `archetype` field,
  which lists it as something to craft

**What an archetype cannot do** is change how stats combine. A double headed tool merges two heads
without stacking their damage, and that lives in `ModularDoubleHeadedItem` because it is code rather
than shape. A type that needs behaviour like that is still a java class. A type that is a set of
slots is a datapack.

## Recipe viewer integration

`se.mickelus.tetra.compat.viewer` reads Tetra's data into shapes a recipe viewer can browse and
imports no viewer api at all. `se.mickelus.tetra.compat.jei` and `se.mickelus.tetra.compat.emi` are
the plugins on top of it, and `MaterialSummary` is the text both of them draw, so a material reads
the same in either viewer.

Both are `compileOnly` and each plugin class is only ever loaded by its own viewer, so a pack with
neither is unaffected. The test pack ships EMI and not JEI.

**Tetra's crafting is not a set of recipes**, which is why only materials are exported. A
schematic's outcome is a function of the target item, the slot and the materials, so exporting
schematics as recipes would be a product of 343 schematics, 78 modules and 107 materials. The
material list is both tractable and the question players actually ask, and the material items are
registered as recipe inputs so that asking what an iron ingot is for finds the page.

**EMI resolves from mavenLocal rather than a remote.** Upstream EMI stops at 1.21.1, so this
builds against EMI Refreshed, the fork in `Projects/Minecraft/EMI/emi-port`. Publish it before
building here, the same as mutil:

```bash
cd "../../EMI/emi-port" && ./gradlew.bat :neoforge:publishToMavenLocal
```

That produces `dev.emi:emi-neoforge:<version>:api`, and `emi_version` in `gradle.properties` names
which one. The version carries the fork's commit hash, so it changes whenever EMI Refreshed is
rebuilt. If the build stops resolving it, republish and update that property.

## Repository rules

1. Minecraft 26.1.2, NeoForge only. Java 25.
2. Credit Mikael Eriksson Vikner as author. Credit EternalHell for the port only, never as author,
   and never a real name.
3. Keep every upstream copyright header.
4. Never mix a port and a bug fix in one commit. A port that also changes behaviour cannot be
   reviewed against upstream, which is the whole value of staying a fork.
5. Upstream issues get fixed after the port, not during.
6. No em dash, no double hyphen in prose, no semicolon, in any document here.
