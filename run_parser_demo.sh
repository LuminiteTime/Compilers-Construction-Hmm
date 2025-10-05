#!/bin/bash

# Parser Demo Runner
# This script compiles and runs the parser demo with all test cases

echo "🔧 Building the parser demo..."
echo "================================="

# Build the project
./gradlew clean build -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"
echo ""

# Compile the demo
echo "🔨 Compiling ParserDemo.java..."
javac -cp "compiler/build/libs/compiler-1.0-SNAPSHOT.jar" ParserDemo.java

if [ $? -ne 0 ]; then
    echo "❌ Demo compilation failed!"
    exit 1
fi

echo "✅ Demo compiled successfully!"
echo ""

# Run the demo
echo "🚀 Running Parser Demo..."
echo "========================="
java -cp ".:compiler/build/libs/compiler-1.0-SNAPSHOT.jar" ParserDemo

echo ""
echo "🎉 Demo completed!"
echo "=================="
