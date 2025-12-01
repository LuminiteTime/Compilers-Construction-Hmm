#!/bin/bash

# Build script for Language I Compiler

set -e

echo "=== Building Language I Compiler ==="

cd "$(dirname "$0")/.."

echo "1. Running Maven build..."
mvn clean package -DskipTests

echo ""
echo "=== Build Complete ==="
echo "JAR location: target/compiler-i-1.0.0.jar"
echo ""
echo "Usage:"
echo "  java -jar target/compiler-i-1.0.0.jar compile <source.i> -o <output.wat>"
echo "  java -jar target/compiler-i-1.0.0.jar run <source.i> -o <output.wat>"
echo "  java -jar target/compiler-i-1.0.0.jar ast <source.i>"

