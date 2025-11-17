# WebAssembly Code Generator

This package contains the code generation phase of the Imperative (I) language compiler. It converts the Abstract Syntax Tree (AST) produced by the C++ parser into WebAssembly (WASM) Text Format (WAT).

## Components

### Core Classes

1. **WasmCodeGenerator** - Main code generator class
   - Manages WASM module generation
   - Maintains symbol tables and scope information
   - Emits WASM instructions

2. **CodeGenVisitor** - AST visitor implementation
   - Traverses AST nodes
   - Dispatches to appropriate generation methods
   - Implements the Visitor pattern

3. **ASTVisitor** - Visitor interface
   - Defines traversal contract
   - Enables clean separation of concerns

### Utility Classes

4. **WasmType** - WASM type enumeration
   - Maps language types to WASM primitives (i32, f64)
   - Provides type conversion utilities

5. **WasmOperator** - Operator mapping
   - Maps language operators to WASM instructions
   - Handles type-specific operations

6. **CodeGenUtils** - General code generation utilities
   - Type system utilities
   - Label and variable name generation
   - Type conversion code emission

7. **MemoryLayout** - Memory management
   - Calculates memory layout for arrays and records
   - Manages heap allocation
   - Handles alignment and padding

### Symbol Table

8. **SymbolInfo** - Symbol metadata
   - Stores variable/function information
   - Tracks WASM indices and memory offsets

9. **CodeGenSymbolTable** - Scope management
   - Manages variable scopes
   - Assigns WASM local/global indices
   - Tracks function declarations

### Output Formatting

10. **WasmPrinter** - Pretty-printer for WAT
    - Formats WASM text with proper indentation
    - Provides high-level instruction emission

### Exceptions

11. **CodeGenException** - Code generation errors
    - Thrown on type errors, undefined symbols, etc.

## Architecture

### Compilation Flow

```
C++ Parser (AST)
        ↓
   Java Bridge
        ↓
   CodeGenVisitor (traverse AST)
        ↓
   WasmCodeGenerator (emit WASM)
        ↓
   WasmPrinter (format output)
        ↓
   WAT File
```

### Type System

Language types are mapped to WASM as follows:

- `integer` → `i32` (32-bit signed)
- `real` → `f64` (64-bit float)
- `boolean` → `i32` (0 = false, 1 = true)
- `array` → `i32` (pointer to memory)
- `record` → `i32` (pointer to memory)

### Memory Layout

```
[0x0000 - 0x0FFF]: Reserved/Runtime data (4KB)
[0x1000 - ...]:    Heap space for dynamic allocations

Array Layout:
[offset+0]: size (i32)
[offset+4]: element[0]
[offset+8]: element[1]
...

Record Layout:
[offset+0]: field_0
[offset+k]: field_1
...
(fields aligned according to their size)
```

## Usage

### Basic Usage

```java
// Create code generator
WasmCodeGenerator generator = new WasmCodeGenerator();

// Generate from AST (once JNI is fully implemented)
CppASTBridge bridge = new CppASTBridge(astPointer, generator);
String wat = bridge.generate();

// Write to file
generator.writeToFile("output.wat");
```

### Direct Instruction Emission

```java
WasmCodeGenerator gen = new WasmCodeGenerator();

// Declare variable
gen.emitLocalDeclaration("x", "integer");

// Emit constant
gen.emitConstant("integer", "42");

// Emit store
gen.emitStore("x");

// Generate module
String wat = gen.generate(null);
```

### Using Symbol Table

```java
CodeGenSymbolTable symTable = new CodeGenSymbolTable();

// Enter scope
symTable.enterScope();

// Declare variable
symTable.declareLocal("counter", "integer");

// Lookup variable
SymbolInfo info = symTable.lookup("counter");
System.out.println("WASM index: " + info.getWasmIndex());

// Exit scope
symTable.exitScope();
```

### Memory Management

```java
MemoryLayout layout = new MemoryLayout();

// Allocate array
int arrayPtr = layout.allocateArray(10, "integer");

// Register record type
MemoryLayout.RecordLayout recordLayout = layout.registerRecord("Point");
recordLayout.addField("x", "real");
recordLayout.addField("y", "real");

// Allocate record
int recordPtr = layout.allocateRecord("Point");
```

## Code Generation Examples

### Variable Declaration

Input:
```
var x: integer is 42;
```

Generated WAT:
```wasm
(local $x i32)
i32.const 42
local.set $x
```

### Binary Operation

Input:
```
x := 2 + 3 * 4;
```

Generated WAT (postfix evaluation):
```wasm
i32.const 2
i32.const 3
i32.const 4
i32.mul
i32.add
local.set $x
```

### While Loop

Input:
```
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```

Generated WAT:
```wasm
(block $break
  (loop $continue
    local.get $counter
    i32.const 0
    i32.le_s
    br_if $break
    
    ;; body
    
    br $continue
  )
)
```

### Array Access

Input:
```
numbers[i]
```

Generated WAT:
```wasm
local.get $numbers
local.get $i
i32.const 1
i32.sub
i32.const 4
i32.mul
i32.add
i32.const 4
i32.add
i32.load
```

## Integration with C++ Parser

The code generator integrates with the C++ Bison parser through:

1. **JNI Bridge** - Direct C++ AST access
2. **Symbol Table Communication** - Shared symbol information
3. **Error Reporting** - Consistent error messages

When fully implemented, the complete compilation pipeline will be:

```
Source Code (.i)
    ↓
Java Lexer
    ↓
C++ Parser
    ↓
Semantic Analyzer
    ↓
Java Code Generator
    ↓
WebAssembly (WAT)
```

## Limitations

1. **JNI Implementation Pending** - Full C++ AST traversal not yet complete
2. **WASI Integration** - Print functions use simplified I/O
3. **No Optimization** - Generated code not optimized
4. **String Literals** - Not directly supported in current implementation

## Future Enhancements

1. Implement full JNI bridge for AST traversal
2. Add optimization passes (constant folding, dead code elimination)
3. Improve print function implementation with proper WASI calls
4. Add debug symbol generation
5. Implement tail-call optimization for recursion
6. Memory pooling and defragmentation

## Testing

Test cases are located in `tests/cases/codegen/`:

- Basic variable declarations
- Array operations
- Record field access
- Control flow (if, while, for)
- Function declarations and calls
- Type conversions
- Expression evaluation

Run tests with:
```bash
./gradlew :tests:test
```

## References

- WebAssembly Core Specification: https://webassembly.org/
- WASM Text Format: https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format
- WABT Tools: https://github.com/WebAssembly/wabt

