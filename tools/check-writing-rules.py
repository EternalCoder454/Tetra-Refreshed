"""Check the writing rules: no em dash and no double hyphen outside code blocks.

Vendored from Minecraft/tools/check-writing-rules.py so that CI can run it. That directory is
shared between projects and is not version controlled, so a runner cannot reach it, and a workflow
step guarded on the file existing would skip in silence and report green.

Keep the two copies identical. If the rules change, change them there and copy again.
"""
import pathlib
import sys

BANNED = {
    "em dash": "—",
    "en dash": "–",
    "double hyphen": "--",
}

failures = 0
for target in sys.argv[1:]:
    path = pathlib.Path(target)
    in_code = False
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line.lstrip().startswith("```"):
            in_code = not in_code
            continue
        if in_code:
            continue
        # A markdown table separator row is required syntax, not prose.
        if set(line.strip()) <= set("|-: "):
            continue
        stripped = line
        # inline code spans are code too
        while "`" in stripped:
            first = stripped.find("`")
            second = stripped.find("`", first + 1)
            if second == -1:
                break
            stripped = stripped[:first] + " " * (second - first + 1) + stripped[second + 1:]
        for label, token in BANNED.items():
            if token in stripped:
                print(f"{path.name}:{number}: {label}: {line.strip()[:90]}")
                failures += 1

print(f"{failures} violation(s)")
sys.exit(1 if failures else 0)
