#!/usr/bin/env python3
"""Build the palette artwork and the atlas source that recolours greyscale modules per material.

Writes four things:

  * the key ramp, the greyscale values a module's artwork is drawn in
  * one palette strip per material named on the command line, derived from the tint that material
    already carries, so its look comes from data it already had
  * greyscale artwork for a module family, quantised from an existing variant to the key ramp
  * the atlas source that tells the game to build one recoloured sprite per material

The ramp is vanilla's own, the one armour trims use, so artwork drawn for either fits both.

Usage:
    python tools/make-palettes.py --materials copper iron \\
        --greyscale item/module/double/head/basic_hammer/metal_left:greyscale_left \\
                    item/module/double/head/basic_hammer/metal_right:greyscale_right
"""
import argparse
import io
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import palette as png

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/tetra")
DATA = os.path.join(ROOT, "src/main/resources/data/tetra")
COLOURS = os.path.join(ROOT, "src/main/java/se/mickelus/tetra/items/modular/ItemColors.java")

# Vanilla's trim ramp, eight evenly spaced greys. The atlas matches a pixel against these exactly,
# so artwork has to be quantised to them and a palette has to give exactly this many colours.
KEY_RAMP = [0xE0, 0xC0, 0xA0, 0x80, 0x60, 0x40, 0x20, 0x00]


def read_colour_table():
    """The named tints from ItemColors, so a palette can be derived from the material's own data."""
    table = {}
    for line in io.open(COLOURS, encoding="utf-8"):
        match = re.search(r'define\(0x([0-9a-fA-F]{6,8})\s*,\s*"([a-z_0-9]+)"\)', line)
        if match:
            table[match.group(2)] = int(match.group(1)[-6:], 16)
    return table


def resolve_tint(data, colours, key):
    """{@return the texture tint of a material, whether it names a colour or gives hex}

    A tint may name an entry in ItemColors or write the hex itself, and the game reads both. The
    tool used to take only the first, which meant it refused every material added since the two it
    was written for.
    """
    tint = (data.get("tints") or {}).get("texture")
    if tint is None:
        raise SystemExit("material %s has no texture tint to build a palette from" % key)
    if tint in colours:
        return colours[tint]
    try:
        return int(str(tint).lstrip("#"), 16)
    except ValueError:
        raise SystemExit("material %s has a texture tint that is neither a known colour nor hex (%r)"
                         % (key, tint))


def material_file(key):
    for base, _, files in os.walk(DATA + "/materials"):
        if key + ".json" in files:
            return os.path.join(base, key + ".json")
    raise SystemExit("no material named " + key)


def write_key_ramp():
    path = os.path.join(ASSETS, "textures/colormap/palette_key.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    png.write(path, [[(v, v, v, 255) for v in KEY_RAMP]])
    return path


def write_material_palette(key, colour):
    """Shade the material's tint across the ramp, brightest step at full tint."""
    r, g, b = (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF
    row = []
    for step in KEY_RAMP:
        scale = step / float(KEY_RAMP[0])
        row.append((int(r * scale), int(g * scale), int(b * scale), 255))
    path = os.path.join(ASSETS, "textures/colormap/material", key + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    png.write(path, [row])
    return path


def write_greyscale(source, target):
    """Quantise an existing variant to the key ramp, keeping transparency as it was.

    The source artwork rarely spans the full range of brightness, so its luminance is stretched
    across the ramp first. Without that the whole image lands on two or three steps and every
    material comes out flat, because the palette can only vary what the ramp distinguishes.
    """
    src = os.path.join(ASSETS, "textures", source + ".png")
    width, height, pixels = png.read(src)

    visible = [png.luminance(p) for row in pixels for p in row if p[3] > 0]
    if not visible:
        raise SystemExit("%s is fully transparent" % source)
    low, high = min(visible), max(visible)
    span = max(1, high - low)

    out = []
    for row in pixels:
        line = []
        for pixel in row:
            if pixel[3] == 0:
                line.append((0, 0, 0, 0))
                continue
            stretched = (png.luminance(pixel) - low) * 255.0 / span
            nearest = min(KEY_RAMP, key=lambda step: abs(step - stretched))
            line.append((nearest, nearest, nearest, pixel[3]))
        out.append(line)
    path = os.path.join(ASSETS, "textures", os.path.dirname(source), target + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    png.write(path, out)
    return path


def write_atlas(textures, materials):
    """Add or replace one source, keeping any others.

    A greyscale texture is offered to one category of material, so the atlas holds one source per
    group rather than one source overall. This used to rewrite the file with a single source, which
    meant running it for metals threw away whatever had been built for woods.

    A source is identified by the textures it covers, so re running the same group updates it in
    place rather than adding a duplicate.
    """
    path = os.path.join(ASSETS, "atlases/items.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)

    existing = []
    if os.path.isfile(path):
        existing = json.load(io.open(path, encoding="utf-8")).get("sources", [])

    source = {
        "type": "minecraft:paletted_permutations",
        "textures": sorted("tetra:" + t for t in textures),
        "palette_key": "tetra:colormap/palette_key",
        "permutations": {key: "tetra:colormap/material/" + key for key in sorted(materials)},
    }

    kept = [s for s in existing if s.get("textures") != source["textures"]]
    kept.append(source)
    kept.sort(key=lambda s: s.get("textures", []))

    io.open(path, "w", encoding="utf-8", newline="\n").write(
        json.dumps({"sources": kept}, indent=4) + "\n")
    return path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--materials", nargs="+", required=True)
    parser.add_argument("--greyscale", nargs="+", required=True,
                        help="source:target pairs, both without textures/ or the extension")
    args = parser.parse_args()

    colours = read_colour_table()
    written = [write_key_ramp()]

    for key in args.materials:
        path = material_file(key)
        data = json.load(io.open(path, encoding="utf-8"))
        written.append(write_material_palette(key, resolve_tint(data, colours, key)))

        # Point the material at its palette. Materials without one are untouched.
        data["palette"] = "tetra:colormap/material/" + key
        # material files are CRLF in this repo, and this tool writing LF is what made the
        # two it had already been run against the only ones out of step with the rest
        io.open(path, "w", encoding="utf-8", newline="\r\n").write(json.dumps(data, indent=4) + "\n")

    targets = []
    for pair in args.greyscale:
        source, target = pair.split(":")
        written.append(write_greyscale(source, target))
        targets.append(os.path.dirname(source) + "/" + target)

    written.append(write_atlas(targets, args.materials))

    for path in written:
        print("  " + os.path.relpath(path, ROOT).replace("\\", "/"))


if __name__ == "__main__":
    main()
