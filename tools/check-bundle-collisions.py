"""
Check what the bundled addons and Tetra both claim.

Tetra embeds two addons with jarJar, and both of them ship into `assets/tetra` and `data/tetra`
because that is where Tetra's stores look. That is the whole reason the arrangement works, and it is
also the one way it can go wrong: when two jars write the same path, load order decides which one a
player gets, and load order is not something either mod controls.

Some collisions are fine and deliberate. Tetra's stores merge rather than replace when a file sets
`"replace": false`, so an addon extending a schematic Tetra already defines is working as intended.
A collision on a texture, or on a data file that does not set `replace` false, is not.

    python tools/check-bundle-collisions.py

Run it after adding an addon or moving a file into `assets/tetra`. It reads the built jar, so build
first, and it says nothing about whether the content is right, only about who wins.
"""
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent

# paths every jar has, which say nothing about content
IGNORED = ("META-INF/", "pack.mcmeta", "pack.png", "LICENSE", "logo.png")


def interesting(name):
    return (not name.endswith("/")
            and not any(name.startswith(p) or name == p for p in IGNORED))


def main():
    jars = sorted(ROOT.glob("build/libs/tetra-*.jar"))
    if not jars:
        print("no built jar, run gradlew build first")
        return 1
    outer = jars[0]

    import io

    contents, opened = {}, {}
    with zipfile.ZipFile(outer) as z:
        contents["tetra"] = {n for n in z.namelist() if interesting(n)}
        opened["tetra"] = z

        bundled = [n for n in z.namelist()
                   if n.startswith("META-INF/jarjar/") and n.endswith(".jar")]
        for path in bundled:
            name = path.rsplit("/", 1)[-1].rsplit("-", 2)[0]
            inner = zipfile.ZipFile(io.BytesIO(z.read(path)))
            contents[name] = {n for n in inner.namelist() if interesting(n)}
            opened[name] = inner

        # A file that sets replace false merges into what is already loaded rather than throwing it
        # away, so it is an extension rather than a clash. The flag has to be read from the jar that
        # ships that copy: Tetra's own is the base and is expected to say true.
        merging = set()
        for name, files in contents.items():
            if name == "tetra":
                continue
            for f in files:
                if not f.endswith(".json"):
                    continue
                try:
                    data = json.loads(opened[name].read(f))
                except (KeyError, ValueError):
                    continue
                # some data files are a bare list, which carries no flag at all
                if isinstance(data, dict) and data.get("replace") is False:
                    merging.add((name, f))

    print("%s" % outer.name)
    for name, files in contents.items():
        print("  %-34s %4d files" % (name, len(files)))

    names = list(contents)
    clashes = {}
    for i, a in enumerate(names):
        for b in names[i + 1:]:
            for f in contents[a] & contents[b]:
                clashes.setdefault(f, set()).update({a, b})

    if not clashes:
        print("\nno two jars claim the same path")
        return 0

    # a clash is settled when every jar but Tetra's merges into it rather than replacing it
    deliberate, real = {}, {}
    for f, who in clashes.items():
        addons = [name for name in who if name != "tetra"]
        if addons and all((name, f) in merging for name in addons):
            deliberate[f] = who
        else:
            real[f] = who

    if deliberate:
        print("\nsame path, merged on purpose because the file sets replace false:")
        for f in sorted(deliberate):
            print("  %-64s %s" % (f, ", ".join(sorted(deliberate[f]))))

    if real:
        print("\nsame path, and load order decides which one a player gets:")
        for f in sorted(real):
            print("  %-64s %s" % (f, ", ".join(sorted(real[f]))))
        print("\nEach of these is either a file that should set replace false, or a duplicate that")
        print("should be deleted from all but the jar that owns it. A texture cannot merge at all.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
