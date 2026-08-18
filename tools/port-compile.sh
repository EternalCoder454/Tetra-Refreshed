#!/usr/bin/env bash
# Compile and write the log where the parsers expect it. Run from the project root.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/port
export TMP=C:/gtmp TEMP=C:/gtmp
./gradlew.bat compileJava --console=plain > build/port/compile.log 2>&1
echo "gradle exit=$?"
python tools/port-errors.py
