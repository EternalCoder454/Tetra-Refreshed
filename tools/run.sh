#!/usr/bin/env bash
# Build, deploy and launch the test pack, then report what the run did.
#
# A compiling jar says nothing about whether the mod loads, and every loading failure so far has
# surfaced only here. Run this rather than trusting a green build.
#
# Usage: bash tools/run.sh [seconds to wait, default 60]
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
WAIT="${1:-60}"
MC="$APPDATA/PrismLauncher/instances/Eternally Dutified/minecraft"
PRISM="$LOCALAPPDATA/Programs/PrismLauncher/prismlauncher.exe"

export TMP=C:/gtmp TEMP=C:/gtmp
if ! ./gradlew.bat build --console=plain 2>&1 | grep -q "BUILD SUCCESSFUL"; then
  echo "BUILD FAILED"
  exit 1
fi

# A crashed instance keeps the jar open, so clear it before replacing.
if ! rm -f "$MC/mods/tetra-"*.jar 2>/dev/null; then
  powershell -c "Get-Process javaw -EA SilentlyContinue | Stop-Process -Force" >/dev/null 2>&1
  sleep 2
  rm -f "$MC/mods/tetra-"*.jar
fi
cp build/libs/tetra-*.jar "$MC/mods/"

"$PRISM" -l "Eternally Dutified" >/dev/null 2>&1 &
sleep "$WAIT"

LOG="$MC/logs/latest.log"
if grep -q "Loading errors encountered" "$LOG" 2>/dev/null; then
  echo "=== LOAD FAILED ==="
  sed -n '/Loading errors encountered/,/^$/p' "$LOG" | head -20
  echo "=== tetra frames ==="
  grep -E "se\.mickelus" "$MC/logs/debug.log" | grep -vE "DEBUG|INFO" | head -6
  exit 1
fi

if grep -qE "Sound engine started|Created: .* minecraft:textures/atlas" "$LOG" 2>/dev/null; then
  echo "=== REACHED THE MENU ==="
  grep -cE "ERROR" "$LOG" | sed 's/^/error lines: /'
  exit 0
fi

echo "=== STILL LOADING after ${WAIT}s ==="
tail -3 "$LOG"
