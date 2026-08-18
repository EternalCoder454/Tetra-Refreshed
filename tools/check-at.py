"""Check every access transformer entry still names something that exists.

An access transformer that names a class or member which moved does not fail the build. It is
simply ignored, and the access it was supposed to widen never happens, so the failure surfaces
much later as a private access error on a line that looks unrelated. That is exactly how the
stale AbstractArrow entry hid here: the class moved into a subpackage during the port and the
entry kept pointing at the old name.

Usage: python tools/check-at.py
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVAP = r"C:\Program Files\Java\jdk-25.0.4\bin\javap.exe"
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.95-merged.jar"
AT = ROOT / "src/main/resources/META-INF/accesstransformer.cfg"

_cache = {}


def members(fqcn):
    """Raw javap output for a class, or None when the class itself is missing."""
    if fqcn not in _cache:
        out = subprocess.run([JAVAP, "-p", "-s", "-cp", str(JAR), fqcn],
                             capture_output=True, text=True)
        _cache[fqcn] = out.stdout if out.returncode == 0 and out.stdout.strip() else None
    return _cache[fqcn]


problems = 0
for n, raw in enumerate(AT.read_text(encoding="utf-8").splitlines(), 1):
    line = raw.split("#")[0].strip()
    if not line:
        continue
    parts = line.split()
    if len(parts) < 2:
        continue
    target = parts[1]
    member = parts[2] if len(parts) > 2 else None

    body = members(target)
    if body is None:
        print(f"  line {n}: class not found  {target}")
        problems += 1
        continue
    if member is None:
        continue

    name = re.match(r"[\w<>$]+", member).group(0)
    descriptor = member[len(name):]
    if name == "<init>":
        name = target.rsplit(".", 1)[-1].replace("$", ".")
        found = name in body
    else:
        found = re.search(r"\b" + re.escape(name) + r"\b", body) is not None
    if not found:
        print(f"  line {n}: member not found  {target} {member}")
        problems += 1
    elif descriptor and descriptor not in body.replace(" ", ""):
        # descriptors are printed on their own line by -s, so check loosely
        if descriptor not in body:
            print(f"  line {n}: descriptor not found  {target} {member}")
            problems += 1

print(f"{problems} stale entr{'y' if problems == 1 else 'ies'}")
sys.exit(1 if problems else 0)
