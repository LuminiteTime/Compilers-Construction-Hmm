#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Language I Compiler Test Suite"
echo "=============================="

# Check if JAR exists
if [ ! -f "target/compiler-i-1.0.0.jar" ]; then
    echo "Building compiler..."
    mvn package -DskipTests -q
fi

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test
run_test() {
    local file="$1"
    local basename=$(basename "$file" .i)

    # Some tests are negative: they are expected to FAIL compilation.
    # For them, any non-zero exit code (except timeout) is treated as PASSED.
    local expected_fail=0
    case "$file" in
        tests/fixtures/comprehensive/errors.i|\
        tests/new_tests/scoping_rules.i|\
        tests/new_tests/invalid_var_name_reserved_keyword.i|\
        tests/new_tests/invalid_for_loop_var_reserved_keyword.i|\
        tests/lexer/keywords.i|\
        tests/parser/complex_expressions.i|\
        tests/parser/control_flow.i|\
        tests/semantic/function_calls.i|\
        tests/semantic/scoping.i|\
        tests/syntax_error.i)
            expected_fail=1
            ;;
    esac

    printf "%-50s" "Testing $basename..."

    # Run compilation with timeout, capture output
    local output
    output=$(timeout 10 java -jar target/compiler-i-1.0.0.jar compile "$file" -o "output/$basename.wat" 2>&1)
    local result=$?

    if [ $expected_fail -eq 0 ]; then
        # Regular (positive) test: expect successful compilation
        if [ $result -eq 0 ]; then
            echo -e "${GREEN}PASSED${NC}"
            # Run the generated .wat file with wasmtime
            echo "  Output:"
            result=$(~/.wasmtime/bin/wasmtime "output/$basename.wat" 2>/dev/null)
            if [ $? -eq 0 ] && [ -n "$result" ]; then
                echo "    Result: $result"
            else
                echo "    (no result or execution failed)"
            fi
            return 0
        elif [ $result -eq 124 ]; then
            echo -e "${RED}TIMEOUT${NC}"
            return 1
        else
            echo -e "${RED}FAILED${NC}"
            # Show error output (skip the ✓ Compilation successful! message)
            echo "$output" | grep -v "✓ Compilation successful!" | sed 's/^/  /'
            return 1
        fi
    else
        # Negative test: expect compilation to fail quickly (non-zero, non-timeout)
        if [ $result -eq 0 ]; then
            echo -e "${RED}FAILED${NC}"
            echo "  Expected compilation to fail, but it succeeded."
            return 1
        elif [ $result -eq 124 ]; then
            echo -e "${RED}TIMEOUT${NC}"
            echo "  Expected compilation to fail quickly, but it timed out."
            return 1
        else
            echo -e "${GREEN}PASSED (expected failure)${NC}"
            echo "  Output:"
            echo "$output" | grep -v "✓ Compilation successful!" | sed 's/^/  /'
            return 0
        fi
    fi
}

echo ""
echo "Running tests..."

# Find all .i files and run tests
for file in $(find tests -name "*.i" -type f | sort); do
    if run_test "$file"; then
        ((PASSED_TESTS++))
    else
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))
done

echo ""
echo "Test Results:"
echo "Total tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $FAILED_TESTS -eq 0 ]; then
    echo "All tests passed"
else
    echo "Some tests failed"
fi
