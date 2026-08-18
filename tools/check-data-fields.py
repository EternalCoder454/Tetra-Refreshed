"""
Check ported data against the codec that actually reads it.

A file that fails to parse is logged and dropped, which is loud and gets noticed. The quiet failure
is a sub object keeping a field name that was renamed. Vanilla codecs ignore unknown fields, so the
constraint disappears and whatever it guarded now always passes. That is how the Long Gone
advancement came to be granted on world join: a location predicate names `structures` now, the file
still said `structure`, and a predicate with nothing left to check matches everywhere.

Comparing against every field name vanilla declares anywhere is not enough, because `structure` is
declared, just on a different type. So this resolves each object to its own codec through the same
registries the game uses, and checks only that codec's fields.

    python tools/check-data-fields.py

Findings are candidates rather than verdicts. Read each one: authoring labels and comments show up
here too, and a field declared through a dispatch helper rather than fieldOf can read as unknown.
"""
import json
import pathlib
import re
import sys
import zipfile
from collections import defaultdict

root = pathlib.Path(__file__).resolve().parent.parent
data = root / "src/main/resources/data/tetra"

jars = sorted((root / "build/moddev/artifacts").glob("minecraft-patched-*-sources.jar"))
if not jars:
    sys.exit("no sources jar, run: ./gradlew.bat build")
jar = zipfile.ZipFile(jars[-1])

by_simple = {}
for entry in jar.namelist():
    if entry.endswith(".java"):
        by_simple.setdefault(entry.rsplit("/", 1)[-1][:-5], entry)

field_re = re.compile(r"(?:optional)?[Ff]ieldOf\(\s*\"([^\"]+)\"")
dispatch_re = re.compile(r"dispatch\w*\(\s*\"([^\"]+)\"\s*,\s*\"([^\"]+)\"")
register_re = re.compile(r'Registry\.register\(\s*\w+\s*,\s*"([\w/]+)"\s*,\s*(\w+)\.(?:MAP_CODEC|CODEC|SERIALIZER)')


def source_of(simple):
    entry = by_simple.get(simple)
    return jar.read(entry).decode("utf-8", "replace") if entry else None


def fields_of(simple, seen=None):
    seen = seen if seen is not None else set()
    if simple in seen:
        return set()
    seen.add(simple)

    text = source_of(simple)
    if text is None:
        return set()

    out = set(field_re.findall(text))
    for a, b in dispatch_re.findall(text):
        out.update((a, b))

    # A codec built inline from another type's map codec contributes that type's fields under no
    # name of its own, so follow those as well as the superclass.
    for embedded in re.findall(r"(\w+)\.MAP_CODEC\b", text):
        if embedded != simple:
            out |= fields_of(embedded, seen)
    if "commonFields(" in text:
        out |= fields_of("LootItemConditionalFunction", seen)
    m = re.search(r"class\s+" + re.escape(simple) + r"\s+extends\s+(\w+)", text)
    if m:
        out |= fields_of(m.group(1), seen)
    return out


def registry_of(holder, suffix="MAP_CODEC"):
    return dict(register_re.findall(source_of(holder) or ""))


registries = {
    "function": registry_of("LootItemFunctions"),
    "condition": registry_of("LootItemConditions"),
    "entry": registry_of("LootPoolEntries"),
    "recipe": registry_of("RecipeSerializers"),
}

# Keys the enclosing structure supplies rather than the dispatched codec.
base = {
    "function": {"function"},
    "condition": {"condition"},
    "entry": {"type", "conditions", "functions", "weight", "quality"},
    "recipe": {"type", "group", "category", "show_notification"},
}

# Below these the contents are freeform rather than codec fields.
opaque = {"components", "custom_data", "nbt", "tag", "entity_data", "block_entity_data"}

# Predicates are plain nested records rather than registry dispatched types, so there is no id in
# the file to resolve them by. They are named by the key that holds them instead. This is the half
# that catches the Long Gone bug, which lived in a location predicate three levels down.
nested = {
    "location": "LocationPredicate",
    "distance": "DistancePredicate",
    "effects": "MobEffectsPredicate",
    "equipment": "EntityEquipmentPredicate",
    "flags": "EntityFlagsPredicate",
    "light": "LightPredicate",
    "block": "BlockPredicate",
    "fluid": "FluidPredicate",
}

cache = {}
findings = defaultdict(list)
foreign = []


def check(node, kind, file, path):
    key = "type" if kind in ("entry", "recipe") else kind
    type_id = node.get(key)
    if not isinstance(type_id, str):
        return
    short = type_id.split(":")[-1]
    cls = registries[kind].get(short)
    if not cls:
        # Mod added types are resolved by their own mod, not by anything readable here.
        foreign.append((file, kind, type_id))
        return
    if cls not in cache:
        cache[cls] = fields_of(cls)
    allowed = cache[cls] | base[kind]
    for k in node:
        if k not in allowed:
            findings[(kind, short, k)].append((file, path))


def check_nested(node, simple, file, path):
    if simple not in cache:
        cache[simple] = fields_of(simple)
    if not cache[simple]:
        return
    for k in node:
        if k not in cache[simple]:
            findings[("predicate", simple, k)].append((file, path))


def walk(node, file, path, parent=None):
    if isinstance(node, dict):
        if parent in nested:
            check_nested(node, nested[parent], file, path)
        if "function" in node and parent == "functions":
            check(node, "function", file, path)
        # An advancement holds its conditions under the name of the entity they apply to, so
        # the key above them is not always "conditions".
        if "condition" in node:
            check(node, "condition", file, path)
        if "type" in node and parent in ("entries", "children"):
            check(node, "entry", file, path)
        for k, v in node.items():
            if k not in opaque:
                walk(v, file, path + "/" + k, k)
    elif isinstance(node, list):
        for item in node:
            walk(item, file, path + "[]", parent)


scanned = 0
for sub in ("loot_table", "loot_modifiers", "recipe", "advancement"):
    for f in sorted((data / sub).rglob("*.json")):
        scanned += 1
        rel = sub + "/" + str(f.relative_to(data / sub)).replace("\\", "/")
        try:
            content = json.loads(f.read_bytes().decode("utf-8"))
        except Exception as e:
            print("UNPARSEABLE", rel, e)
            continue
        if sub == "recipe":
            check(content, "recipe", rel, "")
        walk(content, rel, "")

print("files scanned:", scanned)
print()

if findings:
    print("keys the object's own codec does not read:")
    for (kind, short, k), hits in sorted(findings.items(), key=lambda kv: -len(kv[1])):
        print("  %s %s -> %r  (%d)" % (kind, short, k, len(hits)))
        for file, path in hits[:4]:
            print("       ", file, path)
else:
    print("no unread keys")

print()
print("types resolved by another mod, not checked here:")
for row in sorted(set(foreign)):
    print("  ", *row)

sys.exit(1 if findings else 0)
