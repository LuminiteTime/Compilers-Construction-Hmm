#!/bin/bash

# Script to run parser tests

echo "Building parser..."
make clean
make

if [ $? -ne 0 ]; then
    echo "Build failed"
    exit 1
fi

echo "Running tests..."

for i in {1..10}; do
    echo "Running test$i.i"
    ./parser < test$i.i > test$i.out 2> test$i.err
    echo "Test $i completed. Check test$i.out and test$i.err"
done

echo "All tests completed."