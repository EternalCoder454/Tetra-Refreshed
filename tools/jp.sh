#!/usr/bin/env bash
# javap a class from anywhere on the compile classpath. Usage: bash tools/jp.sh <fqcn> [grep pattern]
#
# The classpath comes from build/port/cp.txt, which `tools/cp.gradle` writes from the configuration
# Gradle actually compiles against. Globbing the cache for a jar by name answered confidently and
# wrongly more than once: an older forge-universal from another project's cache made Capabilities
# read as an empty class, and FMLEnvironment read as absent because it lives in the fancymodloader
# jar rather than in NeoForge. Ask the build, do not guess.
JAVAP="/c/Program Files/Java/jdk-25.0.4/bin/javap.exe"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPFILE="$ROOT/build/port/cp.txt"
if [ ! -f "$CPFILE" ]; then
  echo "no build/port/cp.txt, run: ./gradlew.bat -I tools/cp.gradle dumpCp" >&2
  exit 2
fi
# Paths hold spaces, so join on the separator rather than iterating words.
CP=$(tr '\n' ';' < "$CPFILE")
OUT=$("$JAVAP" -p -cp "$CP" "$1" 2>/dev/null)
if [ -z "$OUT" ]; then echo "NOT FOUND: $1"; exit 1; fi
if [ -n "${2:-}" ]; then echo "$OUT" | grep -i -- "$2"; else echo "$OUT"; fi
