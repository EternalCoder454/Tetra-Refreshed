"""Rewrite the 1.21.1 NBT getters onto the 26.1.2 Optional API.

CompoundTag and ListTag getters return Optional now. Each has an `*Or` sibling taking the
default the old getter returned implicitly, so the port is mechanical -- but only for calls
with the right arity. `Component.getString()` takes none and
`StringArgumentType.getString(ctx, name)` takes two, and neither is an NBT read.
"""
import pathlib, re, sys

# name -> (replacement, default literal, accepted arg counts)
GETTERS = {
    "getInt":     ("getIntOr",     "0",           {1}),
    "getString":  ("getStringOr",  '""',          {1}),
    "getBoolean": ("getBooleanOr", "false",       {1}),
    "getFloat":   ("getFloatOr",   "0.0F",        {1}),
    "getDouble":  ("getDoubleOr",  "0.0",         {1}),
    "getLong":    ("getLongOr",    "0L",          {1}),
    "getByte":    ("getByteOr",    "(byte) 0",    {1}),
    "getShort":   ("getShortOr",   "(short) 0",   {1}),
    "getCompound": ("getCompoundOrEmpty", None,   {1}),
    "getList":     ("getListOrEmpty",     None,   {1, 2}),
    "contains":    ("contains",           None,   {2}),
}

CALL = re.compile(r"\.(" + "|".join(GETTERS) + r")\(")


def split_args(text, start):
    """Top level argument strings and the index just past the closing paren, or None."""
    depth, args, cur, i = 0, [], [], start
    while i < len(text):
        c = text[i]
        if c in "\"'":
            quote, cur, i = c, cur + [c], i + 1
            while i < len(text):
                cur.append(text[i])
                if text[i] == "\\":
                    cur.append(text[i + 1]); i += 2; continue
                if text[i] == quote:
                    i += 1; break
                i += 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                args.append("".join(cur).strip())
                return [a for a in args if a != ""], i + 1
            depth -= 1
        elif c == "," and depth == 0:
            args.append("".join(cur).strip()); cur = []; i += 1; continue
        cur.append(c); i += 1
    return None


def rewrite(src):
    out, pos, count = [], 0, 0
    for m in CALL.finditer(src):
        if m.start() < pos:
            continue
        name = m.group(1)
        # ArrayUtils.contains is commons-lang, not a tag lookup
        if src[max(0, m.start() - 10):m.start()].endswith("ArrayUtils"):
            continue
        parsed = split_args(src, m.end())
        if parsed is None:
            continue
        args, end = parsed
        new, default, arities = GETTERS[name]
        if len(args) not in arities:
            continue
        # getList(key, TAG_TYPE) and contains(key, TAG_TYPE) drop the type argument
        kept = [args[0]] if len(args) == 2 else list(args)
        if default is not None:
            kept.append(default)
        out.append(src[pos:m.start()])
        out.append("." + new + "(" + ", ".join(kept) + ")")
        pos, count = end, count + 1
    out.append(src[pos:])
    return "".join(out), count


root = pathlib.Path("src/main/java")
total = 0
for f in sorted(root.rglob("*.java")):
    s = f.read_text(encoding="utf-8")
    new, n = rewrite(s)
    if n:
        if "--dry" not in sys.argv:
            f.write_text(new, encoding="utf-8")
        total += n
        print(f"{n:3d}  {f.relative_to(root).as_posix()}")
print("call sites rewritten:", total)
