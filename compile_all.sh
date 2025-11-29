#!/bin/bash

echo "Compiling all .i files to .wat..."

SUCCESS_COUNT=0
FAIL_COUNT=0

for file in $(find tests -name "*.i" -type f | sort); do
    # Create output filename
    relative_path=${file#tests/}
    output_file="output/${relative_path%.i}.wat"

    # Create output directory
    mkdir -p "$(dirname "$output_file")"

    echo -n "Compiling $file -> $output_file ... "

    # Compile
    if java -jar target/compiler-i-1.0.0.jar compile "$file" -o "$output_file" >/dev/null 2>&1; then
        echo "✓ SUCCESS"
        ((SUCCESS_COUNT++))
    else
        echo "✗ FAILED"
        ((FAIL_COUNT++))
        # Remove failed output file if it was created
        rm -f "$output_file"
    fi
done

echo ""
echo "Compilation Summary:"
echo "✓ Successful: $SUCCESS_COUNT files"
echo "✗ Failed: $FAIL_COUNT files"
echo "Total: $((SUCCESS_COUNT + FAIL_COUNT)) files"
