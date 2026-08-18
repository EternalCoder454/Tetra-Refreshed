"""Apply a regex only on the lines javac flagged, selected by error message.

A blanket rewrite of a name like `.x` is unsafe -- Vec3 keeps its public fields while ChunkPos
does not. Scoping each substitution to the exact lines in the error list keeps a rename from
reaching a receiver that never had the problem.

Usage: python tools/port_lines.py <error-substring> <regex> <replacement> [--dry]
"""
import pathlib, re, sys

sel, pattern, repl = sys.argv[1], sys.argv[2], sys.argv[3]
dry = "--dry" in sys.argv
root = pathlib.Path("src/main/java")

targets = {}
for raw in pathlib.Path("build/port/errors.txt").read_text(encoding="utf-8").splitlines():
    head, _, detail = raw.partition(" | ")
    path, _, rest = head.partition(":")
    lineno, _, msg = rest.partition(": ")
    if sel in msg or sel in detail:
        targets.setdefault(path, set()).add(int(lineno))

rx = re.compile(pattern)
total = 0
for path, lines in sorted(targets.items()):
    f = root / path
    src = f.read_text(encoding="utf-8").splitlines(keepends=True)
    hits = 0
    for n in sorted(lines):
        if not 0 < n <= len(src):
            continue
        new, k = rx.subn(repl, src[n - 1])
        if k:
            src[n - 1] = new
            hits += k
    if hits:
        if not dry:
            f.write_text("".join(src), encoding="utf-8")
        total += hits
        print(f"{hits:3d}  {path}")
print("substitutions:", total, "(dry run)" if dry else "")
