# -*- coding: utf-8 -*-
"""Boot the dedicated server, load the world, and fail on anything our mods silently dropped.

A green build proves the java compiles. It proves nothing about the data, and the data is where
every failure in this port has actually lived. Forty one advancements, nine loot modifiers, fourteen
scroll icons and thirty attribute modifiers were all dropped on load while the build stayed green,
and every one of them was found by reading a log by hand. This does that reading.

Why the server and not the client. A datapack reload is what parses advancements, recipes, loot
tables and Tetra's own stores, and a dedicated server does the whole of it without a window, a GPU
or a person clicking through a menu. The cost is that it never builds the item atlas, so client only
failures are invisible here. Those are listed under CLIENT_BLIND_SPOT below rather than pretended
about.

Why signatures and not log levels. NeoForge logs 158 @OnlyIn advisories at ERROR on every single
boot of this mod family. A gate that fails on ERROR fails every run, and a gate that fails every run
gets switched off within a week. So every failure here is a named pattern with a reason, and
anything unrecognised is reported without failing.

Usage:
    python tools/boot-gate.py            build first, then boot and check
    python tools/boot-gate.py --no-build use the jar that is already there
    python tools/boot-gate.py --keep-log leave the captured log in place for reading

Exit codes:
    0  the server reached the world and nothing of ours was dropped
    1  something of ours was dropped, or the server never got there
    2  the gate itself could not run
"""
import argparse
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
LOG_DIR = os.path.join(ROOT, "build", "boot-gate")
LOG_PATH = os.path.join(LOG_DIR, "server.log")

# Long temp paths break the gradle daemon, which is rule 2 in BUILDING.md and cost a whole session
# to rediscover. Set it here so the gate does not depend on the caller having done so. It is a
# windows problem only, and forcing a path like this on a linux runner would just be wrong.
GRADLE_TMP = r"C:\gtmp"

IS_WINDOWS = os.name == "nt"
GRADLEW = os.path.join(ROOT, "gradlew.bat" if IS_WINDOWS else "gradlew")

# The mods this repository is responsible for. A failure naming one of these fails the gate. A
# failure naming somebody else's mod is reported and ignored, because we cannot fix it and a gate
# that fails on other people's bugs is a gate nobody trusts.
OURS = (
    "tetra",
    "mutil",
    "art_of_forging",
    "secrets_of_forging_revelations",
    "se.mickelus",
    "net.acetheeldritchking",
)

# Some messages name no mod at all. "Unknown attribute 'generic.movement_speed'" is ours because
# only Tetra's deserializer emits it, but the text says so nowhere, and the first version of this
# gate filed it under other people's mods and passed. So ownership also reads the logger.
#
# Log4j abbreviates the logger to two letters per package, so se.mickelus is se.mi and
# net.acetheeldritchking is ne.ac. NeoForge is ne.ne, which is why the second letters matter.
OUR_LOGGERS = re.compile(r"\[(se\.mi|ne\.ac|se\.mickelus|net\.acetheeldritchking)[.\]]")

# Each entry is (name, pattern, why it means something was dropped).
FAILURES = [
    (
        "data file dropped",
        re.compile(r"Couldn't parse data file"),
        "An advancement, recipe, loot table or module never loaded. The feature it describes "
        "simply does not exist in game.",
    ),
    (
        "stack built during reload",
        re.compile(r"does not have components yet"),
        "Something built an ItemStack while item components were unbound, which only happens in a "
        "deserializer running during a datapack reload. Hold the ids and build the stack later.",
    ),
    (
        "attribute modifier ignored",
        re.compile(r"Unknown attribute '([^']*)'"),
        "A module or improvement carries a modifier naming an attribute nothing registers, so the "
        "modifier is discarded and the module quietly does less than its data says.",
    ),
    (
        "module model type unknown",
        re.compile(r"No deserializer found for module model type"),
        "A module model names a type nothing registers, so that module renders as nothing.",
    ),
    (
        "event listener registered nothing",
        re.compile(r"has no @SubscribeEvent methods"),
        "A class was registered to an event bus but exposes no listener, so the behaviour it was "
        "meant to provide never runs.",
    ),
    (
        "data map target missing",
        re.compile(r"specified in data map for registry .* doesn't exist"),
        "A data map names an object that is not registered, so that entry does nothing.",
    ),
    (
        "mod loading failed",
        re.compile(r"Loading errors encountered"),
        "The loader refused the mod outright.",
    ),
]

# Logged loudly, harmless, and not worth failing over. Each needs a reason, so that this list stays
# a set of decisions rather than a place to bury inconvenient output.
ACCEPTED = [
    (
        "@OnlyIn advisory",
        re.compile(r"@OnlyIn used on"),
        "NeoForge no longer strips members annotated @OnlyIn and says so once per annotated member. "
        "Upstream Tetra annotates 158 of them. It only matters if the stripping was relied upon, "
        "which is a porting question rather than a load failure.",
    ),
]

# Real failure modes this gate cannot see, kept here so nobody mistakes a pass for full coverage.
CLIENT_BLIND_SPOT = [
    "Using missing texture, which is how a corrupt or empty png shows up",
    "Missing sprite, which is how a texture named by a model but absent shows up",
    "Duplicate sprite between atlases",
    "Invalid or unused animation frames in an mcmeta",
]

READY = re.compile(r"""Done \([0-9.]+s\)! For help, type""")
FATAL = re.compile(r"Loading errors encountered|A fatal error has occurred|Exception in server tick loop")


def run(cmd, **kw):
    return subprocess.run(cmd, cwd=ROOT, shell=isinstance(cmd, str), **kw)


def env_with_tmp():
    env = dict(os.environ)
    if IS_WINDOWS:
        env["TMP"] = GRADLE_TMP
        env["TEMP"] = GRADLE_TMP
    return env


def ensure_eula():
    """A dedicated server refuses to start without this, and a fresh checkout has no run directory.

    Belongs to the gate rather than to whatever calls it. Running the server is what needs the file,
    so anything that runs the server should not have to remember. Mojang's EULA is accepted here on
    behalf of whoever is running the gate, which is the same acceptance the local run directory has
    carried since the first server boot in this project.
    """
    eula = os.path.join(ROOT, "run", "eula.txt")
    if os.path.exists(eula):
        return
    os.makedirs(os.path.dirname(eula), exist_ok=True)
    with open(eula, "w", encoding="utf-8") as f:
        f.write("eula=true\n")
    print("wrote %s" % eula)


def build():
    """The addons reach the server through the jar's jarJar, so a stale jar tests stale data."""
    print("building, so the jar carries the current addons")
    r = run([GRADLEW, "build", "--console=plain"],
            env=env_with_tmp(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = r.stdout.decode("utf-8", "replace")
    if "BUILD SUCCESSFUL" not in text:
        print("BUILD FAILED, the gate has nothing to check")
        print("\n".join(text.splitlines()[-25:]))
        return False
    return True


def kill_server():
    """Gradle forks the server, and killing gradle leaves the fork holding the world and the logs.

    A survivor from an earlier run is not a harmless stray. It keeps run/logs/latest.log open, and
    the next boot dies with "Couldn't find Minecraft server thread" that looks nothing like a lock,
    which is exactly how this gate failed the first time it was pointed at broken data.

    Matched on the forked game process specifically. Only it carries the fml system properties. The
    gradle daemons do not name this project at all, so they are never in scope, and a daemon killed
    mid build is how the loopback failure in BUILDING.md section 9 looks from the outside.
    """
    if IS_WINDOWS:
        subprocess.run([
            "powershell", "-NoProfile", "-Command",
            "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
            "Where-Object { $_.CommandLine -like '*fml.*' -and "
            "$_.CommandLine -like '*' + $env:SC_ROOT + '*' } | "
            "ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }",
        ], env=dict(os.environ, SC_ROOT=ROOT),
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    else:
        # Same two conditions as the windows branch, fml properties and this project's path. Done by
        # listing and filtering rather than by one pkill pattern, because a single pattern would
        # have to assume which of the two comes first on the command line, and that is not promised.
        try:
            listing = subprocess.run(["pgrep", "-a", "java"], stdout=subprocess.PIPE).stdout
        except FileNotFoundError:
            return
        for entry in listing.decode("utf-8", "replace").splitlines():
            pid, _, cmdline = entry.partition(" ")
            if "fml." in cmdline and ROOT in cmdline:
                subprocess.run(["kill", "-9", pid],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1)


def boot(timeout):
    """{@return the captured log, once the server reaches the world or gives up}"""
    os.makedirs(LOG_DIR, exist_ok=True)
    ensure_eula()
    # A survivor from a previous run holds the log files, so clear before starting rather than only
    # after finishing. Tidying up after yourself is not enough when the previous run was killed.
    kill_server()
    print("booting the dedicated server, up to %ds" % timeout)

    with open(LOG_PATH, "wb") as sink:
        proc = subprocess.Popen(
            [GRADLEW, "runServer", "--console=plain"],
            cwd=ROOT, env=env_with_tmp(), stdout=sink, stderr=subprocess.STDOUT,
        )

        deadline = time.time() + timeout
        reached = False
        while time.time() < deadline:
            if proc.poll() is not None:
                break
            time.sleep(2)
            try:
                with open(LOG_PATH, encoding="utf-8", errors="replace") as f:
                    text = f.read()
            except OSError:
                continue
            if FATAL.search(text):
                break
            if READY.search(text):
                reached = True
                break

        proc.kill()
    kill_server()

    with open(LOG_PATH, encoding="utf-8", errors="replace") as f:
        return f.read(), reached


def owner(line):
    """{@return whether this failure belongs to us, by what it names or by who logged it}"""
    if OUR_LOGGERS.search(line):
        return True
    lowered = line.lower()
    return any(name in lowered for name in OURS)


def report(text, reached):
    lines = text.splitlines()
    print("\ncaptured %d log lines" % len(lines))

    if reached:
        print("the server reached the world")
    else:
        # Do not stop here. A data error bad enough to be fatal on a server is precisely what this
        # gate exists to catch, and the signatures below say which one it was. Printing the tail of
        # the log instead just shows gradle's epilogue, which names nothing.
        print("\nthe server never reached the world, why:")
        markers = re.compile(
            r"FatalStartupException|Caused by:|being used by another process|"
            r"has locked a portion|Failed to start|Encountered an unexpected exception|"
            r"^\* What went wrong:|^Execution failed for task"
        )
        shown = 0
        for i, line in enumerate(lines):
            if markers.search(line):
                print("   %s" % line.strip()[:170])
                shown += 1
                if shown >= 8:
                    break
        if not shown:
            for line in lines[-10:]:
                print("   %s" % line[:170])

    ours, theirs = {}, {}
    for name, pattern, why in FAILURES:
        for line in lines:
            if pattern.search(line):
                bucket = ours if owner(line) else theirs
                bucket.setdefault(name, []).append(line.strip())

    accepted_counts = {}
    for name, pattern, _ in ACCEPTED:
        n = sum(1 for line in lines if pattern.search(line))
        if n:
            accepted_counts[name] = n

    if accepted_counts:
        print("\naccepted and not failed:")
        for name, n in sorted(accepted_counts.items()):
            print("   %-26s %d" % (name, n))

    if theirs:
        print("\nin other people's mods, reported only:")
        for name, hits in sorted(theirs.items()):
            print("   %-26s %d" % (name, len(hits)))
            for h in hits[:2]:
                print("      %s" % h[:150])

    print("\nnot covered here, the server builds no atlas:")
    for item in CLIENT_BLIND_SPOT:
        print("   %s" % item)

    if not ours:
        if not reached:
            print("\nFAIL: the server did not start, and no dropped content explains it. "
                  "The cause is above rather than in the data.")
            return 1
        print("\nPASS: nothing of ours was dropped")
        return 0

    print("\nFAIL: %d kind(s) of our content was dropped" % len(ours))
    whys = {name: why for name, _, why in FAILURES}
    for name, hits in sorted(ours.items(), key=lambda kv: -len(kv[1])):
        print("\n   %s, %d line(s)" % (name.upper(), len(hits)))
        print("   %s" % whys[name])
        seen = []
        for h in hits:
            trimmed = re.sub(r"^\[[^]]*\] \[[^]]*\] \[[^]]*\]: ", "", h)[:150]
            if trimmed not in seen:
                seen.append(trimmed)
        for s in seen[:6]:
            print("      %s" % s)
        if len(seen) > 6:
            print("      ... and %d more distinct" % (len(seen) - 6))
    return 1


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--no-build", action="store_true", help="use the jar that is already built")
    ap.add_argument("--keep-log", action="store_true", help="say where the captured log is")
    ap.add_argument("--timeout", type=int, default=240, help="seconds to wait for the world")
    args = ap.parse_args()

    if not os.path.exists(GRADLEW):
        print("no gradle wrapper at %s" % GRADLEW)
        return 2

    if not args.no_build and not build():
        return 1

    text, reached = boot(args.timeout)
    code = report(text, reached)

    if args.keep_log or code:
        print("\nfull log: %s" % LOG_PATH)
    return code


if __name__ == "__main__":
    sys.exit(main())
