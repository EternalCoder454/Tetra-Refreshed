"""
Check that every class registered on an event bus actually carries a listener.

Forge accepted `EVENT_BUS.register(...)` for a class with no `@SubscribeEvent` method and quietly
did nothing with it. NeoForge throws:

    java.lang.IllegalArgumentException: class ... has no @SubscribeEvent methods,
    but register was called anyway.

That happens during mod construction, so it is not a warning and not localised. It takes the whole
mod down before it loads, and it names one class, which reads as specific when the mistake is
usually a pattern.

Art of Forging hit it twice on consecutive launches, once for a charged ability that never had a
listener and once for its own mod class, where the only `@SubscribeEvent` in the file sat inside a
comment. Both registrations had been dead since they were written and only became fatal on the port.

    python tools/check-bus-registrations.py                 this project
    python tools/check-bus-registrations.py <dir> [<dir>]   named projects

Three things this has to get right, because the first version of it got each of them wrong:

* **`register(this)` counts.** It resolves to the class the call sits in, which is how the mod class
  registers itself, and is the shape that took Art of Forging down the second time.
* **Comments are not code.** An `@SubscribeEvent` inside a comment is not a listener. Stripping
  comments first is the difference between passing and being right.
* **A listener belongs to a class, not a file.** A nested class carrying one says nothing about the
  outer class, and the mod class with an annotated inner class is exactly that case.

Findings here are real rather than candidates. A registration naming a class with no listener does
nothing at best and stops the mod loading at worst.

**It cannot see through a variable.** `EVENT_BUS.register(overlay)` names nothing this can resolve,
so it is not checked. Tetra does that once, in `ClientSetup.registerOverlay`, for ten overlay
classes, and all ten were checked by hand and carry a listener. A registration like that reached at
client setup rather than construction fails later and further in, so it is worth checking by hand
when one is added.
"""
import pathlib
import re
import sys

BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)
LINE_COMMENT = re.compile(r"//[^\n]*")
STRING = re.compile(r'"(?:\\.|[^"\\])*"')

# register(new Thing()), register(Thing.class), register(this)
REGISTER = re.compile(
    r"EVENT_BUS\.register\(\s*(?:new\s+([\w.]+)\s*\(|([\w.]+)\.class\s*\)|(this)\s*\))")

CLASS_DECL = re.compile(r"\b(?:class|enum|interface|record)\s+(\w+)")


def strip(text):
    """{@return the source with comments and string bodies blanked, newlines preserved}"""
    def blank(m):
        return re.sub(r"[^\n]", " ", m.group(0))
    text = BLOCK_COMMENT.sub(blank, text)
    text = LINE_COMMENT.sub(blank, text)
    return STRING.sub(blank, text)


def classes_with_listeners(text):
    """{@return the names of classes that declare at least one @SubscribeEvent method}

    Walks braces so a listener is credited to the innermost class it sits in, which is what the
    event bus cares about. A nested class carrying one says nothing about the class around it.
    """
    found, stack, depth = set(), [], 0
    i = 0
    while i < len(text):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if stack and stack[-1][1] > depth:
                stack.pop()
        elif text.startswith("@SubscribeEvent", i):
            if stack:
                found.add(stack[-1][0])
            i += len("@SubscribeEvent")
            continue
        else:
            m = CLASS_DECL.match(text, i)
            if m:
                stack.append((m.group(1), depth + 1))
                i = m.end()
                continue
        i += 1
    return found


def enclosing_class(text, position):
    """{@return the innermost class the given offset sits inside, or None}"""
    stack, depth = [], 0
    for m in re.finditer(r"[{}]|\b(?:class|enum|interface|record)\s+(\w+)", text[:position]):
        if m.group(0) == "{":
            depth += 1
        elif m.group(0) == "}":
            depth -= 1
            while stack and stack[-1][1] > depth:
                stack.pop()
        else:
            stack.append((m.group(1), depth + 1))
    return stack[-1][0] if stack else None


def check(root):
    src = root / "src" / "main" / "java"
    if not src.is_dir():
        print("%s\n  no java sources\n" % root.name)
        return 0

    listeners, registrations = set(), []
    for path in sorted(src.rglob("*.java")):
        text = strip(path.read_text(encoding="utf-8", errors="replace"))
        listeners |= classes_with_listeners(text)
        for m in REGISTER.finditer(text):
            named = m.group(1) or m.group(2)
            target = named.rsplit(".", 1)[-1] if named else enclosing_class(text, m.start())
            registrations.append((target, path.name, "this" if m.group(3) else "named"))

    missing = sorted({(cls, where, how) for cls, where, how in registrations
                      if cls and cls not in listeners})

    print("%s" % root.name)
    print("  %d registrations, %d classes carry a listener" % (len(registrations), len(listeners)))
    if not missing:
        print("  every registration names a class that has one")
        return 0

    print("  registered with no @SubscribeEvent on them, which throws during mod construction:")
    for cls, where, how in missing:
        via = " via register(this)" if how == "this" else ""
        print("     %-34s in %s%s" % (cls, where, via))
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
