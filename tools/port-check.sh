#!/usr/bin/env bash
# Guard against the drop-that-looks-like-success. A parse error aborts analysis, so the count
# collapses and reads as near victory. Any of these means the number is not comparable.
cd "$(dirname "${BASH_SOURCE[0]}")/.."
n=$(grep -cE "error: (';' expected|reached end of file|illegal start|class, interface|<identifier> expected|not a statement|'\)' expected)" build/port/compile.log)
if [ "$n" -gt 0 ]; then
  echo "PARSE ERRORS: $n -- the error count is meaningless until these are fixed"
  grep -E "error: (';' expected|reached end of file|illegal start|class, interface|<identifier> expected|not a statement|'\)' expected)" build/port/compile.log | head -20
  exit 1
fi
echo "no parse errors, count is comparable"
