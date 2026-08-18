"""
Check each material's declared tint against the texture of the item it is made from.

A tint is written by hand, and the quiet failure is writing the wrong one. Nothing validates it: a
material with another material's colours parses, loads and renders, it just renders wrong. That is
how mangrove came to be tinted cherry pink. Its file carried a verbatim copy of cherry's two hex
values, and the only way to notice was to look at a mangrove tool and know what it should look like.

So this measures instead. Averaging the ten vanilla plank textures against the tints Tetra declares
for them gives a median ratio of about 1.1 per channel, and Tetra's glyph tint sits at about 0.85 of
its texture tint. Those two numbers describe what the authored tints already do, so a material far
from them is either deliberate or a mistake, and worth a look either way.

    python tools/check-material-tints.py                 report every material, worst first
    python tools/check-material-tints.py mangrove        report one, and print what it should be

Findings are candidates rather than verdicts. A material may reasonably differ from its texture:
obsidian and diorite tint to white on purpose so the artwork shows through, a gem is often drawn
brighter than the item it comes from, and a material whose item is a block rather than an icon is
averaged over a texture nobody sees at that size. Read each one before changing it.

Modded textures are read from the mods folder of the test instance. Point TETRA_MODS elsewhere to
check against a different pack.
"""
import io
import json
import os
import pathlib
import re
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import palette

ROOT = pathlib.Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src" / "main" / "resources"
MATERIALS = RESOURCES / "data" / "tetra" / "materials"
ITEM_COLORS = ROOT / "src/main/java/se/mickelus/tetra/items/modular/ItemColors.java"

TEXTURE_RATIO = 1.10
GLYPH_RATIO = 0.85

# how far a declared tint may sit from the measured one before it is worth reading, as a plain
# euclidean distance in rgb. Set from the spread of the authored tints rather than picked: the
# vanilla woods all land inside 40, and mangrove sat at 190
THRESHOLD = int(os.environ.get("TINT_THRESHOLD", "60"))

APPDATA = os.environ.get("APPDATA", "")
MODS = pathlib.Path(os.environ.get("TETRA_MODS", os.path.join(
    APPDATA, "PrismLauncher", "instances", "Eternally Dutified", "minecraft", "mods")))


def minecraft_jar():
    """{@return the client jar for the version gradle.properties names}"""
    version = "unknown"
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    found = re.search(r"^\s*minecraft_version\s*=\s*(\S+)", text, re.M)
    if found:
        version = found.group(1)
    jar = pathlib.Path(APPDATA) / "PrismLauncher/libraries/com/mojang/minecraft" / version / (
        "minecraft-%s-client.jar" % version)
    return jar if jar.is_file() else None


_jars = None


def jars():
    """{@return an open zip per namespace, vanilla plus whatever the mods folder holds}"""
    global _jars
    if _jars is not None:
        return _jars
    _jars = {}
    vanilla = minecraft_jar()
    if vanilla:
        _jars["minecraft"] = zipfile.ZipFile(vanilla)
    if MODS.is_dir():
        for path in sorted(MODS.glob("*.jar")):
            try:
                z = zipfile.ZipFile(path)
            except zipfile.BadZipFile:
                continue
            # a jar declares its namespaces by the directories it ships under assets
            for name in z.namelist():
                parts = name.split("/")
                if len(parts) > 2 and parts[0] == "assets" and parts[1] not in _jars:
                    _jars[parts[1]] = z
    return _jars


def texture_for(item_id):
    """{@return (zip, path) for the texture drawn for this item, or None}"""
    if ":" not in item_id:
        item_id = "minecraft:" + item_id
    namespace, path = item_id.split(":", 1)
    z = jars().get(namespace)
    if z is None:
        return None
    names = set(z.namelist())
    for kind in ("item", "block"):
        candidate = "assets/%s/textures/%s/%s.png" % (namespace, kind, path)
        if candidate in names:
            return z, candidate
    return None


def average(z, path):
    """{@return the mean rgb of the opaque pixels of a texture inside a jar}"""
    tmp = ROOT / "build" / "_tint.png"
    tmp.parent.mkdir(parents=True, exist_ok=True)
    io.open(tmp, "wb").write(z.read(path))
    try:
        width, height, pixels = palette.read(str(tmp))
    finally:
        os.remove(tmp)

    # a texture taller than it is wide is an animation strip, so only the first frame counts
    if height > width and height % width == 0:
        height = width

    r = g = b = n = 0
    for y in range(height):
        for x in range(width):
            pixel = pixels[y][x]
            if pixel[3] < 128:
                continue
            r += pixel[0]
            g += pixel[1]
            b += pixel[2]
            n += 1
    return (r // n, g // n, b // n) if n else None


_colors = None


def colors():
    """{@return the named colours ItemColors defines, so a tint may name one instead of a hex}"""
    global _colors
    if _colors is None:
        _colors = {}
        if ITEM_COLORS.is_file():
            text = ITEM_COLORS.read_text(encoding="utf-8")
            for name, value in re.findall(r"(\w+)\s*=\s*define\(0x([0-9a-fA-F]{6})", text):
                _colors[name] = int(value, 16)
    return _colors


def as_rgb(tint):
    """{@return the rgb a tint field means, whether it names an ItemColors entry or gives hex}"""
    if tint in colors():
        value = colors()[tint]
    else:
        try:
            value = int(str(tint).lstrip("#"), 16)
        except ValueError:
            return None
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def scaled(rgb, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def hexof(rgb):
    return "%02x%02x%02x" % tuple(rgb)


def distance(a, b):
    return int(sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5)


def main():
    wanted = sys.argv[1] if len(sys.argv) > 1 else None

    if not jars():
        print("no jars to read. Set TETRA_MODS, and check the minecraft version in "
              "gradle.properties has been downloaded")
        return 1

    findings, skipped = [], []
    for path in sorted(MATERIALS.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        key = data.get("key", path.stem)
        if wanted and key != wanted:
            continue

        tints = data.get("tints")
        material = data.get("material") or {}
        items = material.get("items") or []
        if not tints or not tints.get("texture") or not items:
            skipped.append((key, "no tint, or made from a tag rather than named items"))
            continue

        found = texture_for(items[0])
        if not found:
            skipped.append((key, "no texture found for %s" % items[0]))
            continue

        mean = average(*found)
        if mean is None:
            skipped.append((key, "texture is fully transparent"))
            continue

        declared = as_rgb(tints["texture"])
        if declared is None:
            skipped.append((key, "cannot read tint %r" % tints["texture"]))
            continue

        expected = scaled(mean, TEXTURE_RATIO)
        findings.append(dict(key=key, path=path, item=items[0], mean=mean, declared=declared,
                             expected=expected, glyph=scaled(mean, TEXTURE_RATIO * GLYPH_RATIO),
                             off=distance(declared, expected),
                             raw=tints["texture"], raw_glyph=tints.get("glyph", "")))

    findings.sort(key=lambda f: -f["off"])

    if wanted:
        for f in findings:
            print("%s, made from %s" % (f["key"], f["item"]))
            print("  texture average   %s" % hexof(f["mean"]))
            print("  declared          %s / %s" % (f["raw"], f["raw_glyph"]))
            print("  measured          %s / %s" % (hexof(f["expected"]), hexof(f["glyph"])))
            print("  distance          %d" % f["off"])
        for key, why in skipped:
            print("%s skipped: %s" % (key, why))
        return 0

    print("%d materials measured, %d skipped\n" % (len(findings), len(skipped)))

    # a white tint multiplies to nothing, which is the idiom for letting the module artwork show
    # through unchanged. It is never a colour someone meant to measure, so it is not a finding
    white = [f for f in findings if f["declared"] == (255, 255, 255)]
    if white:
        print("tinted white on purpose, so the artwork shows through: %s\n"
              % ", ".join(f["key"] for f in white))

    flagged = [f for f in findings if f["off"] > THRESHOLD and f not in white]
    if flagged:
        print("declared tint sits more than %d from the texture it is made from:\n" % THRESHOLD)
        print("  %-28s %-9s %-9s %-9s %s" % ("material", "declared", "measured", "off", "item"))
        for f in flagged:
            print("  %-28s %-9s %-9s %-9d %s" % (
                f["key"], hexof(f["declared"]), hexof(f["expected"]), f["off"], f["item"]))
        print("\nRun with a material name to see what it would be set to. Read before changing:")
        print("a tint may differ from its texture on purpose, and this cannot tell the difference.")
    else:
        print("every declared tint is within %d of its texture" % THRESHOLD)
    return 0


if __name__ == "__main__":
    sys.exit(main())
