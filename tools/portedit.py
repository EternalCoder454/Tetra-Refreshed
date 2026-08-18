"""Apply literal text replacements to a source file without disturbing its line endings.

Every Java file here is CRLF. Reading with newline preserved makes a multi line search string
written with \n silently fail to match, which reads as a replacement that ran and did nothing.
Read with universal newlines, match against \n, and write back in the file's original ending.

Usage from another script:
    from portedit import edit
    edit("src/.../Foo.java", [(old, new), ...])
Each pair must match exactly once unless a count is given as a third element.
"""
import io, os, sys


def edit(path, pairs):
    with io.open(path, "rb") as f:
        raw = f.read()
    crlf = b"\r\n" in raw
    text = raw.decode("utf-8").replace("\r\n", "\n")
    for pair in pairs:
        old, new = pair[0], pair[1]
        want = pair[2] if len(pair) > 2 else 1
        got = text.count(old)
        if got != want:
            raise SystemExit("%s: expected %d match for %r, found %d" % (path, want, old[:70], got))
        text = text.replace(old, new)
    out = text.replace("\n", "\r\n") if crlf else text
    with io.open(path, "wb") as f:
        f.write(out.encode("utf-8"))
    return path
