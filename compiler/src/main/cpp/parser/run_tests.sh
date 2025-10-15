#!/bin/bash

# Script to run parser tests

echo "Running tests..."

for i in {1..10}; do
    echo "=========================================="
    echo "Running test$i.i"
    echo "=========================================="

    echo "FILE CONTENT:"
    cat test$i.i
    echo "----------------------------------------"
    echo ""

    echo "PARSING STEPS & AST ANALYSIS:"
    ./parser < test$i.i 2>&1
    echo "----------------------------------------"
    echo ""

    echo ""
done

echo "All tests completed."
