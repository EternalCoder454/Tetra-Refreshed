"""
Report materials whose items come from a mod that need not be installed.

Such a material is unobtainable, which is fine and is a normal thing for an addon to ship. What was
not fine is what Tetra did with it: `SimpleItemPredicate` drops item ids it cannot resolve, and a
predicate left with no items and no tag filters by nothing and says yes to everything.

Three of Art of Forging's materials name mods that were not in the pack. One was a fibre with 6.5
hardness, one a metal with 8.5 hardness and a netherite tool level, and both accepted any item that
could go in a slot. A holosphere made a claw, wood wrapped a handle, and hammer tiers came out of
nowhere. The predicate is fixed, so these are inert now rather than universal.

    python tools/check-material-items.py                 this project
    python tools/check-material-items.py <dir> [<dir>]   named projects

This still reports them, because an unobtainable material is worth knowing about even when it is
harmless: it shows in no recipe viewer and can be crafted by nobody, so it is either intentional
compatibility content or a typo in an item id. The two look identical from here, and only the author
can say which it is.

Namespaces belonging to the projects themselves, and to Minecraft and NeoForge, are resolved against
the other projects rather than assumed present.
"""
import io
import json
import os
import pathlib
import sys
import zipfile

# always present, or shipped by these projects
KNOWN = {"minecraft", "neoforge", "c", "tetra", "mutil",
         "secrets_of_forging_revelations", "art_of_forging"}

# A namespace is only missing if the pack does not provide it, so the pack is the authority rather
# than a list kept here. Point TETRA_MODS at a different mods folder to check against another pack.
MODS = pathlib.Path(os.environ.get("TETRA_MODS", os.path.join(
    os.environ.get("APPDATA", ""), "PrismLauncher", "instances", "Eternally Dutified",
    "minecraft", "mods")))


def installed_namespaces():
    """{@return every namespace the mods folder provides, by what the jars ship assets and data for}"""
    found = set(KNOWN)
    if not MODS.is_dir():
        print("no mods folder at %s, checking against the built in list alone" % MODS)
        return found
    for jar in sorted(MODS.glob("*.jar")):
        try:
            archive = zipfile.ZipFile(jar)
        except (zipfile.BadZipFile, OSError):
            continue
        for name in archive.namelist():
            parts = name.split("/")
            if len(parts) > 2 and parts[0] in ("assets", "data"):
                found.add(parts[1])
            elif name.startswith("META-INF/jarjar/") and name.endswith(".jar"):
                try:
                    inner = zipfile.ZipFile(io.BytesIO(archive.read(name)))
                except (zipfile.BadZipFile, OSError):
                    continue
                for nested in inner.namelist():
                    bits = nested.split("/")
                    if len(bits) > 2 and bits[0] in ("assets", "data"):
                        found.add(bits[1])
    return found


def check(root, installed):
    materials = root / "src" / "main" / "resources" / "data" / "tetra" / "materials"
    if not materials.is_dir():
        return 0

    findings = []
    for path in sorted(materials.rglob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except ValueError:
            continue

        material = data.get("material") or {}
        items = material.get("items") or []
        if not items:
            continue  # a tag, or nothing, and neither is this check's problem

        namespaces = {item.split(":")[0] for item in items if ":" in item}
        outside = namespaces - installed
        if outside and namespaces == outside:
            findings.append((data.get("key"), data.get("category"), sorted(outside), items))

    print("%s" % root.name)
    if not findings:
        print("  every material can be obtained from a mod that is present")
        return 0

    print("  materials nobody can obtain, because every item they name is from another mod:")
    for key, category, namespaces, items in findings:
        print("     %-24s category=%-9s from %s" % (key, category, ", ".join(namespaces)))
        for item in items:
            print("        %s" % item)
    return len(findings)


def main():
    roots = ([pathlib.Path(a).resolve() for a in sys.argv[1:]]
             or [pathlib.Path(__file__).resolve().parent.parent])
    installed = installed_namespaces()
    total = 0
    for root in roots:
        total += check(root, installed)
        print()
    if total:
        print("These are inert rather than dangerous, since a predicate that resolves no item now")
        print("matches nothing. Check each one is deliberate compatibility rather than a typo.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
