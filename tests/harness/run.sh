#!/usr/bin/env bash
set -euo pipefail

# Unified test runner for parser/analyzer
# - Discovers tests from tests/harness/testcases.lst
# - Builds the native parser
# - Runs tests with clear, colored output and summary
# - Options:
#     --suite <name>      Run only a specific suite (e.g., analyzer, parser-precedence)
#     --filter <pattern>  Run only tests whose name contains pattern
#     --verbose           Print program output (AST, diagnostics) for every test

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PARSER_DIR="$ROOT_DIR/compiler/src/main/cpp/parser"
CASES_DIR="$ROOT_DIR/tests/cases"

if [ -t 1 ]; then
  GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; CYAN=$'\033[36m'; BOLD=$'\033[1m'; DIM=$'\033[2m'; RESET=$'\033[0m'; CHECK="✓"; CROSS="✗"
else
  GREEN=""; RED=""; YELLOW=""; CYAN=""; BOLD=""; DIM=""; RESET=""; CHECK="OK"; CROSS="X"
fi

SELECT_SUITE=""
FILTER_PATTERN=""
VERBOSE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --suite) SELECT_SUITE="${2:-}"; shift 2;;
    --filter) FILTER_PATTERN="${2:-}"; shift 2;;
    --verbose) VERBOSE=1; shift;;
    *) echo "Unknown option: $1"; exit 2;;
  esac
done

echo -e "${BOLD}${CYAN}=== Building native parser ===${RESET}"
cd "$PARSER_DIR"
make clean >/dev/null 2>&1 || true
if ! make -j"$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)"; then
  echo -e "${RED}${BOLD}Build failed${RESET}"; exit 1
fi
if [ ! -x ./parser ]; then
  echo -e "${RED}${BOLD}Parser binary not found after build${RESET}"; exit 1
fi

echo -e "${BOLD}${CYAN}=== Running tests ===${RESET}"

total=0
failed=0
current_suite=""

run_one() {
  local suite="$1" file_i="$2"
  local base name meta parseErr expects=()
  base="$(basename "$file_i" .i)"
  name="$base"
  meta="${file_i%.i}.meta"
  if [ -n "$FILTER_PATTERN" ]; then
    # Match either the test base name or its relative path (so directory names can be used)
    local rel_path="${file_i#$ROOT_DIR/}"
    if [[ "$name" != *"$FILTER_PATTERN"* && "$rel_path" != *"$FILTER_PATTERN"* ]]; then
      return
    fi
  fi
  [ -n "$SELECT_SUITE" ] && [ "$suite" != "$SELECT_SUITE" ] && return

  if [ "$suite" != "$current_suite" ]; then
    current_suite="$suite"
    echo -e "\n${BOLD}Suite:${RESET} $suite"
    printf '%s\n' "$(printf '%*s' 60 '' | tr ' ' '-')"
  fi

  total=$((total+1))
  local rel_file="$file_i"
  printf "  %-40s %b\n" "$name" "${DIM}(${rel_file#$ROOT_DIR/})${RESET}"

  # defaults
  parseErr=0
  if [ -f "$meta" ]; then
    while IFS= read -r mline || [ -n "$mline" ]; do
      mline="$(printf '%s' "$mline" | tr -d '\r')"
      [[ -z "$mline" || "$mline" =~ ^# ]] && continue
      if [[ "$mline" =~ ^parseErr= ]]; then
        parseErr="${mline#parseErr=}"
      elif [[ "$mline" =~ ^expect: ]]; then
        expects+=("${mline#expect: }")
      fi
    done < "$meta"
  fi

  local out rc ok=1
  set +e
  out=$(./parser < "$rel_file" 2>&1)
  rc=$?
  set -e

  if [ "$parseErr" = "1" ]; then :; else [ $rc -ne 0 ] && ok=0; fi
  for s in "${expects[@]}"; do
    [ -n "$s" ] && ! grep -Fq "$s" <<< "$out" && ok=0 && echo -e "    ${RED}${CROSS}${RESET} expected: ${BOLD}$s${RESET}"
  done

  # Optional verbose output: always show the program output (AST/diagnostics)
  if [ "$VERBOSE" -eq 1 ]; then
    if [ -n "$out" ]; then
      echo -e "    ${DIM}output:${RESET}"
      echo "$out" | sed -e 's/^/      | /'
    else
      echo -e "    ${DIM}(no output)${RESET}"
    fi
  fi

  if [ $ok -eq 1 ]; then
    echo -e "    ${GREEN}${CHECK}${RESET} ${DIM}ok${RESET}"
  else
    echo -e "    ${RED}${CROSS} FAIL${RESET}"
    # If not in verbose mode, still show the output to aid debugging
    if [ "$VERBOSE" -ne 1 ]; then
      echo "$out" | sed -e 's/^/      | /'
    fi
    failed=$((failed+1))
  fi
}

# Discover suites as directories under tests/cases
for suite_dir in "$CASES_DIR"/*; do
  [ -d "$suite_dir" ] || continue
  suite="$(basename "$suite_dir")"
  # Recurse into nested subfolders and process all .i files
  while IFS= read -r -d '' file_i; do
    run_one "$suite" "$file_i"
  done < <(find "$suite_dir" -mindepth 2 -type f -name "*.i" -print0 | sort -z)
done

printf '\n'
printf '%s\n' "$(printf '%*s' 60 '' | tr ' ' '=')"
if [ $failed -eq 0 ]; then
  echo -e "${BOLD}${GREEN}All $total tests passed${RESET}"
else
  echo -e "${BOLD}${RED}$failed/${total} tests failed${RESET}"
fi
exit $failed
