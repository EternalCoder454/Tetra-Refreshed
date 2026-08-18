#!/usr/bin/env bash
# javap a class from Minecraft or NeoForge. Usage: bash tools/jp.sh <fqcn> [grep pattern]
JAVAP="/c/Program Files/Java/jdk-25.0.4/bin/javap.exe"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC="$ROOT/build/moddev/artifacts/minecraft-patched-26.1.2.95-merged.jar"
UJ=$(find "$HOME/.gradle/caches" -name "forge-universal.jar" 2>/dev/null | head -1)
OUT=$("$JAVAP" -p -cp "$MC:$UJ" "$1" 2>/dev/null)
if [ -z "$OUT" ]; then echo "NOT FOUND: $1"; exit 1; fi
if [ -n "${2:-}" ]; then echo "$OUT" | grep -i -- "$2"; else echo "$OUT"; fi
