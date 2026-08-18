import collections, pathlib, re
txt = pathlib.Path(str(pathlib.Path(__file__).resolve().parent.parent / "build/port/compile.log")).read_text(encoding="utf-8", errors="replace")
lines = txt.splitlines()
pat = re.compile(r"Tetra Refreshed.src.main.java.(.+?):(\d+): error: (.+)")
errs = []
for i, line in enumerate(lines):
    m = pat.search(line)
    if not m: continue
    detail = ""
    for j in range(i+1, min(i+6, len(lines))):
        if "symbol:" in lines[j]:
            detail = lines[j].split("symbol:")[1].strip(); break
    errs.append((m.group(1).replace("\\","/"), int(m.group(2)), m.group(3).strip(), detail))
uniq = sorted(set(errs))
print("matched:", len(errs), " unique:", len(uniq))
print("\n== messages ==")
for m,c in collections.Counter(e[2][:64] for e in uniq).most_common(18): print(f"  {c:4d}  {m}")
print("\n== missing symbols ==")
for s,c in collections.Counter(e[3] for e in uniq if e[3]).most_common(18): print(f"  {c:4d}  {s}")
pathlib.Path(str(pathlib.Path(__file__).resolve().parent.parent / "build/port/errors.txt")).write_text("\n".join(f"{f}:{n}: {msg} | {d}" for f,n,msg,d in uniq), encoding="utf-8")
