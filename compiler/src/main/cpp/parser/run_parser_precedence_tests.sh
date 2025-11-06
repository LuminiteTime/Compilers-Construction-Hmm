#!/bin/bash
set -euo pipefail

# Simple parser+analyzer precedence checks using folded outputs as oracle
# Validates that the grammar enforces correct operator precedence & associativity

if [ -t 1 ]; then
  GREEN="\033[32m"; RED="\033[31m"; BOLD="\033[1m"; RESET="\033[0m"
else
  GREEN=""; RED=""; BOLD=""; RESET=""
fi

cd "$(cd "$(dirname "$0")" && pwd)"

failures=0
run_and_expect() {
  local file="$1"; shift
  local -a substrings=("$@")
  echo "------------------------------------------"
  echo -e "${BOLD}PARSER PRECEDENCE TEST:${RESET} $file"
  echo "------------------------------------------"
  echo "INPUT:"; cat "$file"; echo
  local out
  set +e
  out=$(./parser < "$file" 2>&1)
  local rc=$?
  set -e
  echo "$out"
  if [ $rc -ne 0 ]; then
    echo -e "${RED}Parser returned non-zero exit code: $rc${RESET}"
    failures=$((failures+1))
    return
  fi
  for s in "${substrings[@]}"; do
    if ! grep -Fq "$s" <<< "$out"; then
      echo -e "${RED}✗ Expected to find: ${BOLD}$s${RESET}"
      failures=$((failures+1))
    else
      echo -e "${GREEN}✓ Found:${RESET} $s"
    fi
  done
}

# Arithmetic precedence and associativity
run_and_expect analyzer_precedence_arith.i \
  "IntegerLiteral: 7" \
  "IntegerLiteral: 5" \
  "IntegerLiteral: 5" \
  "RealLiteral: 15" \
  "IntegerLiteral: 0" \
  "IntegerLiteral: 9" \
  "IntegerLiteral: 7"

# Unary precedence
run_and_expect analyzer_precedence_unary.i \
  "IntegerLiteral: 5" \
  "IntegerLiteral: -9" \
  "IntegerLiteral: -2"

# Boolean precedence
run_and_expect analyzer_precedence_boolean.i \
  "BooleanLiteral: true" \
  "BooleanLiteral: false" \
  "BooleanLiteral: false" \
  "BooleanLiteral: false" \
  "BooleanLiteral: true"

# Mixed expressions precedence
run_and_expect analyzer_precedence_mixed.i \
  "BooleanLiteral: false" \
  "BooleanLiteral: true" \
  "BooleanLiteral: true"

if [ $failures -gt 0 ]; then
  echo -e "${RED}Parser precedence tests: $failures failure(s)${RESET}" >&2
  exit 1
else
  echo -e "${GREEN}Parser precedence tests: all passed${RESET}"
fi
