#!/bin/bash
set -euo pipefail

# Colors (enabled only when output is a TTY)
if [ -t 1 ]; then
  GREEN="\033[32m"; RED="\033[31m"; YELLOW="\033[33m"; BOLD="\033[1m"; RESET="\033[0m"
else
  GREEN=""; RED=""; YELLOW=""; BOLD=""; RESET=""
fi

echo -e "${BOLD}Running analyzer tests...${RESET}"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

failures=0

run_and_expect() {
  local file="$1"; shift
  local -a substrings=("$@")
  echo "------------------------------------------"
  echo -e "${BOLD}TEST:${RESET} $file"
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

# Run a test where a parse error is expected; success if output contains all substrings
run_and_expect_parse_error() {
  local file="$1"; shift
  local -a substrings=("$@")
  echo "------------------------------------------"
  echo -e "${BOLD}TEST:${RESET} $file"
  echo "------------------------------------------"
  echo "INPUT:"; cat "$file"; echo
  local out
  set +e
  out=$(./parser < "$file" 2>&1)
  local rc=$?
  set -e
  echo "$out"
  # Non-zero rc is acceptable here; just verify the expected parse error text
  for s in "${substrings[@]}"; do
    if ! grep -Fq "$s" <<< "$out"; then
      echo -e "${RED}✗ Expected to find: ${BOLD}$s${RESET}"
      failures=$((failures+1))
    else
      echo -e "${GREEN}✓ Found:${RESET} $s"
    fi
  done
}

# 1) Constant folding + if simplification + while false removal
run_and_expect analyzer_const_and_control.i \
  "Optimizations applied:" \
  "PrintStatement" \
  "IntegerLiteral: 8"

# 2) Routine return type mismatch (avoid parser-enforced arg checks)
run_and_expect analyzer_routine_mismatch.i \
  "error: Routine 'f' return type mismatch"

# 3) Array index type and static bound warning
run_and_expect analyzer_array_checks.i \
  "error: Array index must be integer" \
  "warning: Array index 4 out of bounds [1..3] (static)"

# 4) Record field unknown
run_and_expect analyzer_record_field.i \
  "error: Unknown field 'z' in record"

# 5) Hoisted declaration naming conflict
run_and_expect analyzer_hoist_conflict.i \
  "error: Duplicate variable declaration 'a' in same scope"

# 6) Hoist and fold in then-branch
run_and_expect analyzer_hoist_and_fold.i \
  "IntegerLiteral: 8" \
  "IntegerLiteral: 3" \
  "PrintStatement"

# 7) Nested hoisting with folding
run_and_expect analyzer_hoist_nested.i \
  "IntegerLiteral: 2" \
  "IntegerLiteral: 4" \
  "PrintStatement"

# 8) Else-branch hoist and fold
run_and_expect analyzer_else_hoist_and_fold.i \
  "IntegerLiteral: 4" \
  "PrintStatement"

# 9) While false nested (loop removed)
run_and_expect analyzer_while_false_nested.i \
  "Optimizations applied:" \
  "IntegerLiteral: 42"

# 10) Assignment type mismatch (parse error expected)
run_and_expect_parse_error analyzer_assignment_type_mismatch.i \
  "Parse error" \
  "Type mismatch in assignment"

# 11) Remove unused declaration (no initializer)
run_and_expect analyzer_remove_unused_decl.i \
  "Optimizations applied:"

# 12) Keep unused decl with initializer (folded)
run_and_expect analyzer_keep_decl_with_initializer.i \
  "IntegerLiteral: 3"

# 13) Post-optimization undefined variable at top level
run_and_expect analyzer_postopt_undefined_top_level.i \
  "error: Undefined variable 'y'"

# 14) Boolean folding
run_and_expect analyzer_boolean_folding.i \
  "BooleanLiteral: true"

# 15) Field access on non-record
run_and_expect analyzer_field_nonrecord.i \
  "error: Field access on non-record type"

# 16) If condition must be boolean (parse error expected)
run_and_expect_parse_error analyzer_if_condition_typecheck.i \
  "Parse error" \
  "If condition must be boolean"

# 17) While condition must be boolean (parse error expected)
run_and_expect_parse_error analyzer_while_condition_typecheck.i \
  "Parse error" \
  "While condition must be boolean"

# 18) Record type duplicate field
run_and_expect analyzer_record_field_duplicate.i \
  "error: Duplicate field 'x' in type 'Point'"

# 19) For-loop over numeric range OK
run_and_expect analyzer_for_range_ok.i \
  "ForLoop" \
  "PrintStatement"

# 20) For-loop numeric range type error (non-integer bound)
run_and_expect analyzer_for_range_type_error.i \
  "error: For range bounds must be integers"

# 21) For-in over array OK
run_and_expect analyzer_for_in_array_ok.i \
  "ForLoop" \
  "PrintStatement"

# 22) Print with multiple expressions
run_and_expect analyzer_print_multiple.i \
  "PrintStatement" \
  "IntegerLiteral: 1" \
  "IntegerLiteral: 2" \
  "IntegerLiteral: 3"

# 23) Routine call: undefined routine (parse error expected)
run_and_expect_parse_error analyzer_routine_call_undefined.i \
  "Parse error" \
  "Undefined routine"

# 24) Routine call: arity mismatch (parse error expected)
run_and_expect_parse_error analyzer_routine_call_arity_mismatch.i \
  "Parse error" \
  "Argument mismatch"

# 25) Routine call: parameter type mismatch (analyzer error)
run_and_expect analyzer_routine_call_type_mismatch.i \
  "error: Argument type mismatch in call to 'f' at position 1" \
  "error: Argument type mismatch in call to 'f' at position 2"

if [ $failures -gt 0 ]; then
  echo -e "${RED}Analyzer tests: $failures failure(s)${RESET}" >&2
  exit 1
else
  echo -e "${GREEN}Analyzer tests: all passed${RESET}"
fi
