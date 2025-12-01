#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Run all integration tests by compiling them with the Language I compiler
# and executing the resulting WAT files via wasmtime (through the `run` command).
#
# Prerequisites:
#   - ./scripts/build.sh has been run, so target/compiler-i-1.0.0.jar exists
#   - `wasmtime` is available on PATH

JAR="target/compiler-i-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "Compiler JAR not found at $JAR. Run ./scripts/build.sh first." >&2
  exit 1
fi

TOTAL=0
PASSED=0
FAILED=0

mkdir -p output/integration

for file in $(find tests/integration -name "*.i" -type f | sort); do
  base=$(basename "$file" .i)
  wat="output/integration/${base}.wat"

  case "$base" in
    bubble_sort|bubble_sort_debug|bubble_sort_step_debug|bubble_swap_debug|\
    insertion_sort|insertion_sort_step_debug|\
    selection_sort|selection_sort_step_debug|\
    gnome_sort|gnome_sort_step_debug)
      printf "%-40s" "Running $base..."
      echo "SKIPPED (sorting tests disabled)"
      continue
      ;;
  esac

  printf "%-40s" "Running $base..."

  # First, compile to WAT only
  compile_output=$(java -jar "$JAR" compile "$file" -o "$wat" 2>&1)
  compile_status=$?

  if [ $compile_status -ne 0 ]; then
    echo "FAILED"
    echo "  Compile error:"
    echo "$compile_output" | sed 's/^/    /'
    FAILED=$((FAILED+1))
    TOTAL=$((TOTAL+1))
    continue
  fi

  # Then, run the WAT via wasmtime and capture program stdout
  program_output=$(wasmtime "$wat" 2>&1)
  run_status=$?

  # Normalize actual output (remove whitespace)
  actual=$(printf "%s" "$program_output" | tr -d ' \t\r\n')

  # Expected outputs for each integration test (concatenated, no spaces/newlines)
  expected=""
  case "$base" in
    array_reverse)            expected="54321" ;;
    array_stats)              expected="25195" ;;
    array_sum)                expected="15" ;;
    bubble_sort_debug)         expected="514231123411234112341123411234" ;;
    bubble_sort)              expected="5142312345" ;;
    insertion_sort)           expected="5142312345" ;;
    selection_sort)           expected="3152412345" ;;
    gnome_sort)               expected="2531412345" ;;
    collatz_steps)            expected="814" ;;
    factorial)                expected="120" ;;
    fibonacci)                expected="" ;;
    forward_functions)        expected="6" ;;
    gcd)                      expected="614" ;;
    matrix)                   expected="6" ;;
    matrix_2x3)               expected="123456" ;;
    matrix_3x3)               expected="123456789" ;;
    matrix_4x4)               expected="12345678910111213141516" ;;
    matrix_multiply_2x2)      expected="44108" ;;
    multi_function_program)   expected="1310" ;;
    nested_records_integration) expected="3010520711001100340" ;;
    big_integration)          expected="30105207115011502580015536661" ;;
    power_function)           expected="3281" ;;
    prime_check_function)      expected="0110" ;;
    primes_sieve)              expected="235711131719" ;;
    records_array_integration) expected="6622" ;;
    sum_even_numbers)          expected="110" ;;
    array_sizeless_and_for_iter) expected="151554321" ;;
    records_reference_semantics) expected="10201993" ;;
    for_range_reverse)        expected="1234554321" ;;
    function_short_body)      expected="61" ;;
    scope_shadowing)          expected="13" ;;
  esac

  TOTAL=$((TOTAL+1))

  if [ $run_status -ne 0 ]; then
    echo -e "${RED}FAILED${NC}"
    echo "  Runtime error:"
    echo "$program_output" | sed 's/^/    /'
    FAILED=$((FAILED+1))
    continue
  fi

  # If we have an expected stdout, compare it with the actual program output
  if [ -n "$expected" ] && [ "$actual" != "$expected" ]; then
    echo -e "${RED}FAILED${NC}"
    echo "  Output mismatch:"
    echo "    expected: $expected"
    echo "    actual:   $actual"
    FAILED=$((FAILED+1))
  else
    echo -e "${GREEN}PASSED${NC}"
    PASSED=$((PASSED+1))
  fi

done

echo
echo "Integration WAT Test Results:"
echo "  Total:  $TOTAL"
echo -e " ${GREEN}Passed: $PASSED${NC}"
echo -e " ${RED}Failed: $FAILED${NC}"

if [ $FAILED -ne 0 ]; then
  exit 1
fi

exit 0
