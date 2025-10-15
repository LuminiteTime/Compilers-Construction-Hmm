# Testing Guide for Lexer and Parser

This document provides comprehensive instructions for testing the lexer and parser components of the Imperative (I) language compiler.

## Overview

The compiler has two main components that need testing:

1. **Java Lexer** - Tokenizes source code into tokens
2. **C++ Parser** - Parses tokens into an Abstract Syntax Tree (AST)
3. **Integration Tests** - End-to-end testing of the complete system

Both individual components and the integrated system have automated test suites and manual testing capabilities.

## Java Lexer Testing

### Automated Unit Tests

The lexer is tested using JUnit 5 unit tests located in `tests/src/test/java/compiler/hmm/TestLexer.java`.

#### Running the Tests

```bash
# From the project root directory
cd tests
./gradlew test
```

Or from the root:

```bash
./gradlew :tests:test
```

#### Test Structure

The `TestLexer` class contains individual test methods that validate specific language features:

- `testVariableDeclarations()` - Variable declarations with and without type inference
- `testArraysDataStructures()` - Array declarations, assignments, and access
- `testRecordTypes()` - Record type definitions and field access
- `testWhileLoops()` - While loop syntax
- `testForLoops()` - For loops with ranges and reverse iteration
- `testFunctionsRecursion()` - Function declarations and recursive calls
- `testTypeConversions()` - Type conversion declarations
- `testErrorDetection()` - Lexical error detection (currently tests valid syntax)
- `testOperatorPrecedence()` - Expression operator precedence
- `testComplexDataStructures()` - Complex nested types and structures
- `testNestedRecords()` - Multi-level record nesting

#### How Lexer Tests Work

Each test method:

1. Defines a source code string in the Imperative (I) language
2. Uses the `tokenize()` helper method to convert the string to tokens
3. Compares the actual tokens against expected `Token` objects
4. Validates token types, lexemes, and position information (line/column)

Example test structure:

```java
@Test
public void testVariableDeclarations() throws LexerException {
    String sourceCode = """
        var x: integer is 42;
        var y: real is 3.14;
        """;

    List<Token> expected = Arrays.asList(
        new Token(TokenType.VAR, "var", 1, 1),
        new Token(TokenType.IDENTIFIER, "x", 1, 5),
        // ... more expected tokens
        new Token(TokenType.EOF, "", 2, 23)
    );

    List<Token> actual = tokenize(sourceCode);
    assertEquals(expected, actual);
}
```

#### Test Output

Test results are generated in `tests/build/reports/tests/test/index.html` with detailed pass/fail information for each test case.

### Manual Lexer Testing

You can also test the lexer manually by creating a simple test program:

```java
import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.LexerException;

public class ManualLexerTest {
    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer(new java.io.StringReader("var x: integer is 42;"));
            Token token;
            while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
                System.out.println(token);
            }
        } catch (LexerException e) {
            System.err.println("Lexer error: " + e.getMessage());
        }
    }
}
```

## C++ Parser Testing

### Automated Parser Tests

The parser is tested using a shell script that runs the parser on multiple test input files.

#### Prerequisites

Ensure you have the required build tools installed:

- `g++` (C++ compiler)
- `bison` (parser generator)
- `flex` (lexer generator)
- `make`

#### Running the Tests

```bash
# From the parser directory
cd compiler/src/main/cpp/parser
./run_tests.sh
```

This script will:

1. Clean previous builds
2. Compile the parser using `make`
3. Run the parser on each test file (`test1.i` through `test10.i`)
4. Generate output files (`test1.out`, `test1.err`, etc.)

#### Test Files

The parser has 10 test input files covering different language features:

1. **test1.i** - Variable declarations with explicit types and type inference
2. **test2.i** - Arrays and data structures
3. **test3.i** - Record types and field access
4. **test4.i** - While loops
5. **test5.i** - For loops with ranges and reverse iteration
6. **test6.i** - Functions and recursion
7. **test7.i** - Type conversions
8. **test8.i** - Error detection (should generate semantic errors)
9. **test9.i** - Operator precedence
10. **test10.i** - Complex data structures

#### Test File Formats

Each test case generates three files that provide complete test traceability:

**📁 `.i` Files - Input Source Code**
- Contains the Imperative (I) language source code to be parsed
- Plain text files with language constructs, declarations, expressions, etc.
- Example: Variable declarations, loops, functions, type definitions

**📋 `.out` Files - Standard Output**
- Captures parser status messages and execution flow
- **Typical Content**:
  - 🟢 `"JavaLexer initialized"` - Lexer startup phase
  - 🔄 `"Generating WASM from AST"` - Code generation phase
  - ✅ `"Parsing completed successfully"` - Successful parsing
  - ❌ `"Parsing failed"` - Parse errors encountered
  - 🟠 `"JavaLexer destroyed"` - Cleanup phase

**⚠️ `.err` Files - Standard Error**
- Contains error messages, diagnostics, and warnings
- **Typical Content**:
  - ❌ `"Parse error at line X: syntax error"` - Grammar violations
  - ⚠️ `"Parse error at line X: Undefined variable"` - Symbol table issues
  - 🔍 `"Unknown character: X"` - Lexer recognition failures

#### Expected Behavior

- **Tests 1-7, 9-10**: Should parse successfully and generate AST output
- **Test 8**: Should report type errors during parsing/semantic analysis

#### Test Output Analysis

After running tests, check the generated files:

- `testN.out` - Parser output (AST construction results)
- `testN.err` - Error messages (should be empty for successful tests except test8)

### Manual Parser Testing

You can test the parser manually on custom input:

```bash
# Build the parser first
cd compiler/src/main/cpp/parser
make

# Test with custom input
echo "var x: integer is 42;" | ./parser

# Or test with a file
./parser < my_test_file.i
```

#### Understanding Parser Output

The parser will output:

- AST construction progress
- Symbol table operations
- Type checking results
- Any syntax or semantic errors

### Building the Parser

If you need to rebuild the parser after making changes:

```bash
cd compiler/src/main/cpp/parser
make clean  # Remove old build artifacts
make        # Build fresh
```

## Integration Testing

### Full Compiler Pipeline

To test the complete lexer → parser pipeline:

1. **Java Lexer Integration**: The parser uses JNI to interface with the Java lexer
2. **Token Flow**: Java lexer produces tokens → C++ parser consumes them
3. **AST Generation**: Parser builds AST with semantic analysis

### Testing the Integration

```bash
# Build both Java and C++ components
./gradlew build
cd compiler/src/main/cpp/parser && make

# The parser's JNI integration allows Java lexer tokens to be consumed
# Test files demonstrate this integration
```

## Troubleshooting

### Common Issues

#### Lexer Tests Failing

- Check that the lexer correctly handles whitespace and comments
- Verify token position tracking (line/column numbers)
- Ensure maximal munch is implemented for operators like `:=`, `<=`, etc.

#### Parser Build Failures

- Ensure `bison`, `flex`, and `g++` are installed
- Check JNI includes path (`JAVA_HOME` environment variable)
- Verify all source files are present

#### Parser Runtime Errors

- Check for undefined symbols in the symbol table
- Verify type compatibility in assignments and expressions
- Look for syntax errors in test input files

### Debug Output

#### Lexer Debug

Add debug prints in the lexer to see tokenization:

```java
System.out.println("Token: " + token.getType() + " '" + token.getLexeme() + "' @ " +
                   token.getLine() + ":" + token.getColumn());
```

#### Parser Debug

The parser generates detailed error messages. Check `testN.err` files for issues.

### Test Coverage

Current test coverage includes:

**Lexer:**

- All token types (keywords, identifiers, literals, operators, delimiters)
- Position tracking
- Error handling
- Maximal munch behavior

**Parser:**

- All grammar rules
- AST construction
- Symbol table management
- Basic type checking
- Error recovery

## Adding New Tests

### Adding Lexer Tests

1. Add a new test method to `TestLexer.java`
2. Follow the pattern: source code → expected tokens → assertion
3. Include various language constructs
4. Test edge cases and error conditions

### Adding Parser Tests

1. Create a new `testN.i` file in the parser directory
2. Add corresponding handling in `run_tests.sh` if needed
3. Ensure the test covers new language features
4. Update this documentation

## Integration Testing

### Overview

Integration tests verify that the Java lexer and C++ parser work together correctly as a complete compiler system.

### Running Integration Tests

```bash
# Run the comprehensive integration test
./integration_test.sh
```

This script performs:
1. **Java Component Testing**: Compiles and runs lexer unit tests
2. **C++ Component Testing**: Builds and runs parser test suite
3. **System Integration Check**: Verifies both components are available and compatible

### Integration Test Results

The integration test provides a summary:

```
=== INTEGRATION TEST ===

1. Testing Java Lexer...
✓ Java lexer tests passed

2. Testing C++ Parser...
✓ C++ parser compiled successfully
✓ Parser tests: 10/10 passed

3. Integration Status...
✓ Both Java and C++ components are available
✓ JNI integration code is present
✓ Integration framework is ready
```

### Manual Integration Testing

For manual testing of specific features:

```bash
# Test a specific source file with the parser
cd compiler/src/main/cpp/parser
./parser test1.i

# Expected output:
# JavaLexer initialized
# Generating WASM from AST
# Parsing completed successfully
# JavaLexer destroyed
```

### Integration Architecture

```
Source File (.i)
    ↓
Java Lexer (Tokenization)
    ↓
Token Stream → C++ Parser (Bison)
    ↓
AST Construction + Semantic Analysis
    ↓
WASM Code Generation (Stub)
```

### Troubleshooting Integration Issues

- **JNI Library Not Found**: Ensure `libparser.so` is in `LD_LIBRARY_PATH`
- **Java Compilation Errors**: Run `./gradlew clean build`
- **Parser Build Errors**: Check `JAVA_HOME` environment variable
- **Test Failures**: Review individual component logs

## Performance Considerations

- Lexer uses manual character-by-character scanning for efficiency
- Parser uses LALR(1) parsing with Bison for optimal performance
- Symbol table uses hash-based lookups for O(1) access
- Tests are designed to run quickly for continuous integration

## Continuous Integration

Both lexer and parser tests can be integrated into CI/CD pipelines:

```bash
# Run all tests
./gradlew :tests:test
cd compiler/src/main/cpp/parser && ./run_tests.sh

# Check for failures in test reports
```

This testing approach ensures the compiler components work correctly both individually and as an integrated system.
