#!/bin/bash

echo "Running all .wat files with wasmtime..."

SUCCESS_COUNT=0
FAIL_COUNT=0

for wat_file in $(find output -name "*.wat" -type f | sort); do
    echo -n "Running $wat_file ... "

    # Run with wasmtime, capture output
    if timeout 10 wasmtime run "$wat_file" >/dev/null 2>&1; then
        echo "✓ SUCCESS"
        ((SUCCESS_COUNT++))
    else
        echo "✗ FAILED/TIMEOUT"
        ((FAIL_COUNT++))
    fi
done

echo ""
echo "Execution Summary:"
echo "✓ Successful: $SUCCESS_COUNT files"
echo "✗ Failed/Timeout: $FAIL_COUNT files"
echo "Total: $((SUCCESS_COUNT + FAIL_COUNT)) files"
