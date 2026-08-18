# Developer guide

Building, running, and the data formats. For the port itself see [PORT-STATUS.md](PORT-STATUS.md),
and for where the design is going see [MODERNIZATION.md](MODERNIZATION.md).

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
python ../../tools/check-mixin-targets.py "Mickelus Mods/Tetra Refreshed"
python ../../tools/check-writing-rules.py <file>         # prose rules for the docs here
```

`port-check.sh` exists because a parse error aborts analysis and collapses the error count into
something that reads like near success. Run it before believing any number.

`check-at.py` matters because a stale access transformer entry is ignored rather than failing the
build, so the widening silently never happens and surfaces much later as a private access error
somewhere unrelated.

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

A material that needs a look nothing else has needs artwork for every module, which is why 268
module textures ship. The palette below is the way out of that.

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

## Repository rules

1. Minecraft 26.1.2, NeoForge only. Java 25.
2. Credit Mikael Eriksson Vikner as author. Credit EternalHell for the port only, never as author,
   and never a real name.
3. Keep every upstream copyright header.
4. Never mix a port and a bug fix in one commit. A port that also changes behaviour cannot be
   reviewed against upstream, which is the whole value of staying a fork.
5. Upstream issues get fixed after the port, not during.
6. No em dash, no double hyphen in prose, no semicolon, in any document here.
