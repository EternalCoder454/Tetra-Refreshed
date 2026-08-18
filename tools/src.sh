#!/usr/bin/env bash
# Print a Minecraft class's decompiled source. Usage: bash tools/src.sh <fqcn-with-slashes-or-dots>
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SJ="$ROOT/build/moddev/artifacts/minecraft-patched-26.1.2.95-sources.jar"
P=$(echo "$1" | tr '.' '/')
unzip -p "$SJ" "$P.java" 2>/dev/null || echo "NOT FOUND: $P.java"
