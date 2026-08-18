"""
Check that every class registered on an event bus actually carries a listener.

Forge accepted `EVENT_BUS.register(new Thing())` for a class with no `@SubscribeEvent` method and
quietly did nothing with it. NeoForge throws:

    java.lang.IllegalArgumentException: class ... has no @SubscribeEvent methods,
    but register was called anyway.

That happens during mod construction, so it is not a warning and it is not localised. It takes the
whole mod down before it loads, and the message names one class, which makes it look specific when
the mistake is usually a pattern.

Art of Forging registered SonicShockEffect that way. It is a charged ability and nothing else, so it
never had a listener, and the registration had been dead since it was written. It reaches the game
through `ItemModularHandheld.registerAbility` instead.

    python tools/check-bus-registrations.py                 this project
    python tools/check-bus-registrations.py <dir> [<dir>]   named projects

Findings here are real rather than candidates: a registration that names a class with no listener
does nothing at best, and stops the mod loading at worst. Either delete it, or the class was meant
to have a listener and does not.
"""
import pathlib
import re
import sys

# `register(new Thing())` and `register(Thing.class)`, which are the two shapes in use here
REGISTER = re.compile(r"EVENT_BUS\.register\(\s*(?:new\s+(\w+)\s*\(|(\w+)\.class\s*\))")


def check(root):
    src = root / "src" / "main" / "java"
    if not src.is_dir():
        return 0

    listeners, registrations = set(), []
    for path in src.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        if "@SubscribeEvent" in text:
            listeners.add(path.stem)
        for m in REGISTER.finditer(text):
            registrations.append((m.group(1) or m.group(2), path.name))

    missing = sorted({(cls, where) for cls, where in registrations if cls not in listeners})

    print("%s" % root.name)
    print("  %d registrations, %d classes carry a listener" % (len(registrations), len(listeners)))
    if not missing:
        print("  every registration names a class that has one")
        return 0

    print("  registered with no @SubscribeEvent on them, which throws during mod construction:")
    for cls, where in missing:
        print("     %-34s registered in %s" % (cls, where))
    return len(missing)


def main():
    roots = ([pathlib.Path(a).resolve() for a in sys.argv[1:]]
             or [pathlib.Path(__file__).resolve().parent.parent])
    total = 0
    for root in roots:
        total += check(root)
        print()
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())
