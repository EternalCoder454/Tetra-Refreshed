import pathlib, sys
root = pathlib.Path(__file__).resolve().parent.parent / "src/main/java"
want = sys.argv[1] if len(sys.argv) > 1 else None
for raw in pathlib.Path(str(pathlib.Path(__file__).resolve().parent.parent / "build/port/errors.txt")).read_text(encoding="utf-8").splitlines():
    head, _, detail = raw.partition(" | ")
    path, _, rest = head.partition(":")
    if want and want not in path: continue
    n = int(rest.split(":")[0])
    src = (root / path).read_text(encoding="utf-8", errors="replace").splitlines()
    print(f"{path}:{n}  [{detail}]")
    print("      " + (src[n-1].strip()[:110] if 0 < n <= len(src) else "?"))
