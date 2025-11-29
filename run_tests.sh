#!/bin/bash

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

    printf "%-50s" "Testing $basename..."

    # Run compilation with timeout, capture output
    local output
    output=$(timeout 10 java -jar target/compiler-i-1.0.0.jar compile "$file" -o "output/$basename.wat" 2>&1)
    local result=$?

    if [ $result -eq 0 ]; then
        echo "PASSED"
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
        echo "TIMEOUT"
        return 1
    else
        echo "FAILED"
        # Show error output (skip the ✓ Compilation successful! message)
        echo "$output" | grep -v "✓ Compilation successful!" | sed 's/^/  /'
        return 1
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
