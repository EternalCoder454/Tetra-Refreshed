"""
Check that every module model declares a type Tetra can deserialize.

`ModuleModelRegistry` holds a deserializer per model type and throws when it meets one it does not
know:

    com.google.gson.JsonParseException: No deserializer found for module model type: draw_0

That throw comes out of gson in the middle of parsing a module or an improvement, so the message
names the type and nothing else. It does not say which of six hundred files declared it, and until
mutil started dropping files one at a time it took the whole store with it.

Tetra registers three types, in `TetraMod`: `tetra:grid_texture`, `tetra:filtered_grid_texture` and
`tetra:shield`. A model with no `type` at all defaults to `tetra:grid_texture`, so leaving it out is
fine and is what most files do.

    python tools/check-model-types.py                 this project
    python tools/check-model-types.py <dir> [<dir>]   named projects

Two mistakes this catches, both of which shipped:

* **A model name in the type field.** `{"type": "tetra:banner/kite"}` where Tetra's own say
  `{"type": "tetra:shield", "model": "tetra:banner/kite"}`.
* **A bare word with no namespace**, like `draw_0`, which was never a type at all.

Findings are real rather than candidates. A type outside the registry throws when the file is read.
"""
import json
import pathlib
import sys

# registered in TetraMod, plus the default a model with no type at all falls back to
REGISTERED = {"tetra:grid_texture", "tetra:filtered_grid_texture", "tetra:shield"}

SEARCHED = ("modules", "improvements", "schematics", "materials", "replacements", "repairs")


def model_types(node, found):
    """Collect the `type` of every object appearing in a `models` array, however deeply nested."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "models" and isinstance(value, list):
                for entry in value:
                    if isinstance(entry, dict) and isinstance(entry.get("type"), str):
                        found.add(entry["type"])
            model_types(value, found)
    elif isinstance(node, list):
        for entry in node:
            model_types(entry, found)


def check(root):
    data = root / "src" / "main" / "resources" / "data" / "tetra"
    if not data.is_dir():
        print("%s\n  no tetra data\n" % root.name)
        return 0

    offenders, scanned = {}, 0
    for directory in SEARCHED:
        for path in (data / directory).rglob("*.json") if (data / directory).is_dir() else []:
            scanned += 1
            try:
                parsed = json.loads(path.read_text(encoding="utf-8"))
            except ValueError:
                continue  # a file that is not json at all is a different checker's problem
            found = set()
            model_types(parsed, found)
            for kind in found - REGISTERED:
                offenders.setdefault(kind, []).append(
                    str(path.relative_to(root / "src/main/resources")).replace("\\", "/"))

    print("%s" % root.name)
    print("  %d files scanned" % scanned)
    if not offenders:
        print("  every model type is one Tetra registers")
        return 0

    print("  model types Tetra has no deserializer for, which throw when the file is read:")
    for kind in sorted(offenders):
        print("     %s" % kind)
        for where in sorted(offenders[kind]):
            print("        %s" % where)
    return sum(len(v) for v in offenders.values())


def main():
    roots = ([pathlib.Path(a).resolve() for a in sys.argv[1:]]
             or [pathlib.Path(__file__).resolve().parent.parent])
    total = 0
    for root in roots:
        total += check(root)
        print()
    if total:
        print("Registered types are %s, and a model with no type defaults to tetra:grid_texture."
              % ", ".join(sorted(REGISTERED)))
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())
