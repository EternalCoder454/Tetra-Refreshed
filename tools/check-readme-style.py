"""The README style, and the check for it. This file is the only statement of it.

Every mod in this tree had a README written in its own shape, so a reader arriving at the second
one had to learn it again. The style below is the one Liteminer Refreshed already used, lifted out
so the rest can share it rather than each inventing a layout.

Read it without opening the code:

    python check-readme-style.py --style

Check a README:

    python check-readme-style.py README.md

What it checks is only the structure. Tone, ordering and which sections a given mod needs are
judgement, and are described by --style rather than enforced. Writing rules are a separate concern,
see check-writing-rules.py.

Copies live in each project that has to run this without reaching the shared tools directory, which
is not version controlled. They are byte identical. If the style changes, change it here and copy.
"""
import pathlib
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="replace")

SKELETON = """# <Mod Name>

<One or two lines saying what it does, in plain words, to somebody who has never heard of it.
Not what it is. What it does for them.>

![Minecraft](https://img.shields.io/badge/minecraft-26.1.2-brightgreen.svg)
![Loader](https://img.shields.io/badge/loader-NeoForge-orange.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## <emoji> About

What it is, and whose it is. If it is a fork or a port, say so here and link the original.

## <emoji> Installing

What to drop where, what else is required, and the Java version.

## <emoji> Using it

Numbered steps for the common case. Bold the words that appear in the game.

## <emoji> Settings, Commands, Shapes, and so on

One section per thing worth its own table. Tables carry a "What it does" column.

## <emoji> If something goes wrong

Where it logs, and the one or two failures a user will actually hit.

## <emoji> Credit

Who wrote the original, under what licence, and what this repository is responsible for.

## <emoji> For developers

One line pointing at DEV.md. Nothing else.
"""

GUIDANCE = """The shape

  A single H1 on the first line, the mod's name and nothing else.
  Then one or two lines of hook, in plain words, saying what it does for the reader.
  Then the badges. Minecraft version, loader, licence, in that order.
  Then sections, each heading led by an emoji.
  Last section points at DEV.md and stops.

The tone

  Second person. You, not the user.
  Say what it does before what it is.
  Bold the words that appear on screen, so a reader can match them to the game.
  Tables when there is a list of things with a value and a meaning, prose otherwise.
  A "What it does" column beats a "Description" column.
  Numbers and defaults belong in the table, not in a sentence about the table.

What belongs in a README and not in DEV.md

  Anything a player does. Installing, using, configuring, and what to do when it misbehaves.
  Credit and licensing, always, and for a fork that comes before anything else.
  Build instructions do not. They live in DEV.md, and the README links to it once.

Emoji

  One per heading, chosen for what the section is about rather than for decoration.
  The same section gets the same emoji across mods, so a reader recognises it.
  Common ones already in use across this tree:

    About            a symbol of the mod itself, a pickaxe, an anvil, a book
    Installing       package
    Using it         game controller
    Settings         gear
    Commands         speech balloon
    Tags or data     label
    Troubleshooting  bandage
    Credit           memo
    For developers   laptop
"""

BADGE = re.compile(r"!\[[^\]]*\]\(https://img\.shields\.io/")
HEADING = re.compile(r"^##\s+(.*)$")
# A heading counts as styled when it opens with something that is not a letter, digit or space.
LED_BY_EMOJI = re.compile(r"^[^\w\s]")


def print_style():
    print("README style. This file is the source, no document restates it.\n")
    print(GUIDANCE)
    print("A skeleton to copy:\n")
    print(SKELETON)


def check(targets):
    failures = 0
    for target in targets:
        path = pathlib.Path(target)
        lines = path.read_text(encoding="utf-8").splitlines()
        problems = []

        if not lines or not lines[0].startswith("# "):
            problems.append("first line is not an H1 with the mod name")

        # the hook, meaning prose between the H1 and the badges
        hook = []
        for line in lines[1:]:
            if line.startswith("![") or line.startswith("## "):
                break
            if line.strip():
                hook.append(line)
        if not hook:
            problems.append("no hook line under the title, saying what it does")

        if not any(BADGE.search(line) for line in lines):
            problems.append("no shields.io badges")

        headings = [m.group(1) for m in (HEADING.match(line) for line in lines) if m]
        if not headings:
            problems.append("no level two sections")
        bare = [h for h in headings if not LED_BY_EMOJI.match(h)]
        for h in bare:
            problems.append("heading not led by an emoji: %s" % h[:60])

        if not any("DEV.md" in line for line in lines):
            problems.append("nothing points at DEV.md")

        for p in problems:
            print("%s: %s" % (path.name, p))
        failures += len(problems)

    print("%d problem(s)" % failures)
    return 1 if failures else 0


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--style"]
    if "--style" in sys.argv[1:] or not args:
        print_style()
        sys.exit(0)
    sys.exit(check(args))
