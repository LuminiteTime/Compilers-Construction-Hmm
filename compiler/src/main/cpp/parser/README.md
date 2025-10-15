# Bison Parser for Imperative (I) Language

This directory contains the Bison-based parser implementation for the Imperative (I) language, as specified in the project requirements.

## Files

- `parser.y`: Bison grammar file with semantic actions for AST construction
- `ast.h`: Abstract Syntax Tree node definitions
- `ast.cpp`: AST implementation
- `symbol.h`: Symbol table classes and utilities
- `symbol.cpp`: Symbol table implementation
- `lexer.h`: Interface for Java lexer integration
- `lexer.cpp`: Stub implementation of JavaLexer
- `lexer.l`: Flex lexer for testing (stub)
- `Makefile`: Build script
- `run_tests.sh`: Test runner script
- `test*.i`: Test input files (10 test cases)
- `README.md`: This documentation

## Building

```bash
make
```

Requirements: bison, flex, g++, make

## Testing

```bash
./run_tests.sh
```

This will run all 10 test cases and generate output files.

## Test Cases

1. **Variable Declarations**: Basic var declarations with and without type inference
2. **Arrays & Data Structures**: Array declarations, assignments, and access
3. **Record Types**: Record type definitions and field access
4. **While Loops**: While loop with condition and body
5. **For Loops**: For loops with ranges and reverse iteration
6. **Functions & Recursion**: Routine declarations with arrow syntax
7. **Type Conversions**: Implicit type conversions
8. **Error Detection**: Type mismatches (should generate errors)
9. **Operator Precedence**: Expression precedence testing
10. **Complex Data Structures**: Nested types, arrays of records, iteration

## Expected Behavior

- Test 1-7, 9-10: Should parse successfully and generate AST
- Test 8: Should report type errors during parsing

## Integration

The parser is designed to integrate with:
- Java-based lexer via JNI (lexer.h/lexer.cpp is stub)
- WASM code generator (WASMGenerator stub in parser.y)

## Notes

- The lexer.l is a stub Flex lexer for testing. Real integration uses JavaLexer.
- Type checking is basic; full semantic analysis would require more implementation.
- Memory management is not implemented (no delete calls).
- Error recovery is minimal.
