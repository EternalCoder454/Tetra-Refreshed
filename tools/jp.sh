#!/usr/bin/env bash
# javap a class from Minecraft or NeoForge. Usage: bash tools/jp.sh <fqcn> [grep pattern]
#
# The patched Minecraft jar carries no NeoForge classes, so those come from the NeoForge jar the
# build actually resolves. Pinning it to the version in gradle.properties matters: an older
# forge-universal from another project's cache answers confidently and wrongly, which is how
# Capabilities read as an empty class for a while.
JAVAP="/c/Program Files/Java/jdk-25.0.4/bin/javap.exe"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NEO_VERSION=$(grep '^neo_version=' "$ROOT/gradle.properties" | cut -d= -f2 | tr -d '\r')
MC="$ROOT/build/moddev/artifacts/minecraft-patched-${NEO_VERSION}-merged.jar"
NF=$(find "$HOME/.gradle/caches/modules-2" -name "neoforge-${NEO_VERSION}-universal.jar" 2>/dev/null | head -1)
if [ -z "$NF" ]; then
  echo "no neoforge-${NEO_VERSION}-universal.jar found, run a build first" >&2
fi
OUT=$("$JAVAP" -p -cp "$MC:$NF" "$1" 2>/dev/null)
if [ -z "$OUT" ]; then echo "NOT FOUND: $1"; exit 1; fi
if [ -n "${2:-}" ]; then echo "$OUT" | grep -i -- "$2"; else echo "$OUT"; fi
