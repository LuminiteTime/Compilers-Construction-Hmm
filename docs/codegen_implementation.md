# Code Generation Implementation Guide

This document describes the complete code generation phase implementation for the Imperative (I) language compiler.

## Overview

The code generator is the final phase of the compiler pipeline:

```
Source Code (.i)
    ↓
Java Lexer (Tokenization)
    ↓
C++ Parser (Syntax Analysis)
    ↓
Semantic Analyzer (Type Checking & Optimization)
    ↓
Java Code Generator (AST → WASM)
    ↓
WebAssembly (WAT)
    ↓
(Optional) WASM Tools (Binary conversion, optimization)
```

## Architecture

### Java Components (`compiler/src/main/java/compiler/codegen/`)

1. **WasmCodeGenerator** - Main generator class
   - Manages WASM module generation
   - Emits WASM instructions
   - Maintains symbol tables and scope information

2. **CodeGenVisitor** - AST visitor
   - Implements visitor pattern for AST traversal
   - Coordinates code generation for each node type
   - Tracks current function context

3. **ASTVisitor** - Visitor interface
   - Defines contract for AST traversal
   - Enables clean separation of concerns

4. **Symbol Table Components**
   - `SymbolInfo` - Individual symbol information
   - `CodeGenSymbolTable` - Scope management and symbol tracking

5. **Utility Classes**
   - `WasmType` - Type system utilities
   - `WasmOperator` - Operator mapping
   - `CodeGenUtils` - General utilities
   - `MemoryLayout` - Memory management for arrays/records
   - `WasmPrinter` - Pretty-printer for WAT

6. **Exception Handling**
   - `CodeGenException` - Code generation errors

### C++ Components (`compiler/src/main/cpp/parser/`)

1. **codegen_bridge.cpp** - JNI bridge for code generation
   - Connects Java code generator with C++ AST
   - Implements native methods called from Java
   - Provides AST traversal and WASM emission

## Type System

The language types are mapped to WASM as follows:

| Language Type | WASM Type | Bytes | Representation |
|---|---|---|---|
| integer | i32 | 4 | 32-bit signed |
| real | f64 | 8 | 64-bit IEEE 754 |
| boolean | i32 | 4 | 0 = false, 1 = true |
| array | i32 | 4 | Pointer to memory |
| record | i32 | 4 | Pointer to memory |

### Type Conversion

Automatic conversions:
- `integer` → `real`: `f64.convert_i32_s`
- `real` → `integer`: `i32.trunc_f64_s` (truncates fractional part)
- `integer` → `boolean`: Check if non-zero (i32.const 0, i32.ne)

## Memory Layout

### Linear Memory Organization

```
[0x0000 - 0x0FFF]: Reserved/Runtime data (4KB)
[0x1000 - ...]:    Heap space for dynamic allocations
```

### Array Layout (1-based indexing)

```
Offset | Content
-------|----------
0      | size (i32) - number of elements
4      | element[1]
8      | element[2]
...    | ...
4*(n+1)| element[n]
```

For real arrays, elements are 8 bytes each:
```
0      | size (i32)
4      | padding
8      | element[1] (f64)
16     | element[2] (f64)
...
```

### Record Layout

Records are laid out with fields in declaration order, with alignment:

```
Example: type Point is record
           var x: real;    ;; 8 bytes, aligned to 8
           var y: real;    ;; 8 bytes, aligned to 8
         end

Offset | Content
-------|--------
0      | x (f64)
8      | y (f64)
Total  | 16 bytes
```

## Code Generation Examples

### Variable Declaration

**Source:**
```
var x: integer is 42;
```

**Generated WAT:**
```wasm
(local $x i32)
i32.const 42
local.set $x
```

### Global Variable

**Source (top-level):**
```
var counter: integer is 0;
```

**Generated WAT:**
```wasm
(global $counter (mut i32) (i32.const 0))
```

### Binary Operation

**Source:**
```
result := 2 + 3 * 4;
```

**Generated WAT (postfix):**
```wasm
i32.const 2
i32.const 3
i32.const 4
i32.mul
i32.add
local.set $result
```

### Function Declaration

**Source:**
```
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end
```

**Generated WAT:**
```wasm
(func $factorial (param $n i32) (result i32)
  (local $temp i32)
  
  local.get $n
  i32.const 1
  i32.le_s
  if (result i32)
    i32.const 1
  else
    local.get $n
    local.get $n
    i32.const 1
    i32.sub
    call $factorial
    i32.mul
  end
)
```

### While Loop

**Source:**
```
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```

**Generated WAT:**
```wasm
(block $break_while_1
  (loop $continue_while_1
    ;; condition (negated for break)
    local.get $counter
    i32.const 0
    i32.le_s
    br_if $break_while_1
    
    ;; body
    local.get $counter
    call $print_int
    
    local.get $counter
    i32.const 1
    i32.sub
    local.set $counter
    
    br $continue_while_1
  )
)
```

### For Loop with Range

**Source:**
```
for i in 1..10 loop
    print i;
end
```

**Generated WAT:**
```wasm
(local $i i32)
i32.const 1
local.set $i

(block $break_for_1
  (loop $continue_for_1
    ;; condition check
    local.get $i
    i32.const 10
    i32.gt_s
    br_if $break_for_1
    
    ;; body
    local.get $i
    call $print_int
    
    ;; increment
    local.get $i
    i32.const 1
    i32.add
    local.set $i
    
    br $continue_for_1
  )
)
```

### Array Operations

**Source:**
```
var numbers: array[5] integer;
numbers[1] := 10;
```

**Generated WAT:**
```wasm
;; Allocate array (size field + 5 elements * 4 bytes = 24 bytes)
i32.const 24
call $alloc
local.set $numbers

;; Store array size
local.get $numbers
i32.const 5
i32.store

;; Store element: numbers[1] := 10
local.get $numbers      ;; base pointer
i32.const 1             ;; index (1-based)
i32.const 1
i32.sub                 ;; convert to 0-based
i32.const 4
i32.mul                 ;; multiply by element size
i32.add
i32.const 4
i32.add                 ;; skip size field
i32.const 10
i32.store
```

### Record Field Access

**Source:**
```
type Point is record
    var x: real;
    var y: real;
end

var p: Point;
p.x := 1.5;
```

**Generated WAT:**
```wasm
;; Allocate record (16 bytes: 2 * f64)
i32.const 16
call $alloc
local.set $p

;; Store field: p.x := 1.5
local.get $p            ;; base pointer
i32.const 0             ;; field offset
i32.add
f64.const 1.5
f64.store
```

## Implementation Workflow

### Step 1: Initialize Code Generator

```java
WasmCodeGenerator generator = new WasmCodeGenerator();
CodeGenSymbolTable symbolTable = new CodeGenSymbolTable();
```

### Step 2: Process Declarations

For each top-level declaration:
1. Create appropriate code generation context
2. Emit declaration code
3. Update symbol table

### Step 3: Generate Functions

For each function:
1. Create new scope
2. Declare parameters
3. Emit local variables
4. Generate function body
5. Exit scope

### Step 4: Generate Statements

For each statement:
- Assignment: evaluate RHS, emit store
- Print: evaluate expression, emit call to print function
- While: block/loop structure with condition
- For: initialize, block/loop with condition
- If: conditional branch
- Return: emit value and return instruction

### Step 5: Generate Expressions

Recursively generate expression code:
- Literals: emit constants
- Variables: emit loads
- Binary ops: generate operands, emit operation
- Unary ops: generate operand, emit operation
- Function calls: emit arguments, emit call

### Step 6: Finalize Module

1. Close module
2. Write to file or return as string
3. Optionally validate with WABT

## Integration with C++ Parser

### JNI Method Mapping

```
C++ Function                    Java Method
================================================
generateWasmFromAST()    ←→    CppASTBridge.generateWasmFromAST()
getASTAsJson()           ←→    CppASTBridge.getASTAsJson()
```

### Build Integration

The Makefile includes code generation support:

```bash
# Build with code generation
cd compiler/src/main/cpp/parser
make clean && make

# Result: libparser.so with JNI support
```

### Usage from Main Compiler

```java
// In Compiler.java
WasmCodeGenerator codeGen = new WasmCodeGenerator();
CppASTBridge bridge = new CppASTBridge(astPointer, codeGen);
String wat = bridge.generate();
Files.writeString(Paths.get(outputFile), wat);
```

## Error Handling

Code generation errors are reported through `CodeGenException`:

- Type mismatches (caught but shouldn't happen if semantic analysis worked)
- Undefined symbols (caught but shouldn't happen)
- Invalid memory access patterns
- Stack depth violations

Example:
```java
try {
    String wat = generator.generate(ast);
} catch (CodeGenException e) {
    System.err.println("Code generation error: " + e.getMessage());
    System.exit(1);
}
```

## Validation

Generated WASM can be validated using WABT tools:

```bash
# Validate WAT syntax
wasm-validate output.wat

# Convert to binary
wat2wasm output.wat -o output.wasm

# Disassemble binary to text
wasm2wat output.wasm -o output_disasm.wat
```

## Testing

Test cases are in `tests/cases/codegen/`:

```
tests/cases/codegen/
├── basic/
│   ├── variables.i
│   ├── expressions.i
│   └── functions.i
├── arrays/
│   ├── allocation.i
│   ├── access.i
│   └── iteration.i
├── records/
│   ├── definition.i
│   ├── access.i
│   └── nesting.i
└── control_flow/
    ├── if_then_else.i
    ├── while_loop.i
    └── for_loop.i
```

Run tests:
```bash
./gradlew :tests:test
```

## Performance Considerations

1. **Code Size** - Generated WASM text format can be large; consider binary format for distribution

2. **Stack Machine** - WASM uses a value stack; code generator carefully manages stack depth

3. **Local Variables** - Prefer locals to globals for performance

4. **Memory Access** - Array/record access requires address calculation; consider caching frequently accessed values

5. **Function Calls** - Each call incurs overhead; consider inlining for hot functions

## Known Limitations

1. JNI integration still being completed
2. Print functions use simplified I/O (full WASI support pending)
3. No optimization passes (could be added)
4. String literals not yet supported
5. No debug symbol generation

## Future Enhancements

1. **Optimization** - Constant folding, dead code elimination, inlining
2. **Debug Info** - Source map generation for debugging
3. **String Support** - Immutable string literals
4. **WASI Integration** - Full I/O support
5. **Tail Calls** - Detect and optimize tail-recursive functions
6. **Memory Optimization** - Pooling and defragmentation

## References

- WASM Specification: https://webassembly.org/
- WASM Text Format: https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format
- WABT Documentation: https://github.com/WebAssembly/wabt
- Compiler Design: Dragon Book, Chapter 8 (Code Generation)

