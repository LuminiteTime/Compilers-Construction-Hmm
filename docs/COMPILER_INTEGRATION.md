# Complete Compiler Integration Guide

This document explains how all compiler phases work together: Lexer → Parser → Semantic Analyzer → Code Generator.

## Complete Compilation Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    Source Code (.i file)                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
                  ┌────────────────────┐
                  │   Java Lexer       │
                  │ (Tokenization)     │
                  └────────┬───────────┘
                           │
                    Token Stream
                           │
                           ▼
                  ┌────────────────────┐
                  │   C++ Parser       │
                  │ (Bison + Flex)     │
                  └────────┬───────────┘
                           │
                      Abstract Syntax Tree (AST)
                           │
                           ▼
                  ┌────────────────────┐
                  │ Semantic Analyzer  │
                  │ (Type checking)    │
                  │ (Optimization)     │
                  └────────┬───────────┘
                           │
                      Optimized AST
                           │
                           ▼
              ┌─────────────────────────────┐
              │   Java Code Generator       │
              │ (AST → WebAssembly)         │
              └─────────┬───────────────────┘
                        │
                   WAT (Text Format)
                        │
                        ▼
          ┌────────────────────────────┐
          │  Optional: WABT Tools      │
          │  - wasm-validate           │
          │  - wat2wasm                │
          │  - wasm-objdump            │
          └─────────┬──────────────────┘
                     │
                     ▼
          ┌────────────────────┐
          │  WASM Binary (.wasm)       
          │  or WAT Text (.wat)        │
          └────────────────────┘
```

## Build System

### Prerequisites

```bash
# Linux (Ubuntu/Debian)
sudo apt-get install build-essential bison flex openjdk-21-jdk

# macOS (with Homebrew)
brew install gcc bison flex openjdk@21

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux
export JAVA_HOME=$(/usr/libexec/java_home)            # macOS
```

### Build Steps

#### Step 1: Build Java Components (Lexer + Codegen)

```bash
cd /home/lanebo1/Compilers-Construction-Hmm

# Build with Gradle
./gradlew build

# This compiles:
# - Java lexer (compiler/src/main/java/compiler/lexer/)
# - Java code generator (compiler/src/main/java/compiler/codegen/)
# - Test suite (tests/src/test/java/)
```

#### Step 2: Build C++ Components (Parser + Analyzer)

```bash
cd compiler/src/main/cpp/parser

# Clean and build
make clean
make

# This generates:
# - parser (executable for testing)
# - libparser.so (shared library for JNI)
```

#### Step 3: Run Full Compiler

```bash
# From project root
java -cp compiler/build/libs/*:compiler/src/main/java \
    compiler.Compiler test.i -o test.wat
```

## Component Details

### Phase 1: Lexical Analysis (Java)

**Location:** `compiler/src/main/java/compiler/lexer/`

**Components:**
- `Lexer.java` - Main lexer class
- `Token.java` - Token representation
- `TokenType.java` - Token enumeration
- `LexerException.java` - Error handling

**Process:**
1. Read source code character by character
2. Recognize tokens (keywords, identifiers, operators, literals)
3. Create token stream with position information
4. Report lexical errors

**Example:**
```
Input: var x: integer is 42;
Output: [VAR, IDENTIFIER("x"), COLON, INTEGER, IS, INTEGER_LITERAL(42), SEMICOLON]
```

### Phase 2: Syntactic Analysis (C++)

**Location:** `compiler/src/main/cpp/parser/`

**Components:**
- `parser.y` - Bison grammar rules
- `lexer.l` - Flex lexer rules
- `ast.h/cpp` - AST node classes
- `symbol.h/cpp` - Symbol table implementation
- `analyzer.h/cpp` - Semantic analyzer

**Process:**
1. Parse token stream according to grammar rules
2. Build Abstract Syntax Tree (AST)
3. Create symbol table for variable/function declarations
4. Report syntax errors

**Generated Files:**
- `parser.tab.c/.h` - Bison parser
- `lex.yy.c` - Flex lexer
- `libparser.so` - Shared library for JNI

### Phase 3: Semantic Analysis (C++)

**Location:** `compiler/src/main/cpp/parser/analyzer.cpp`

**Checks Performed:**
1. **Type Checking**
   - Variable declarations have consistent types
   - Assignment target and value types are compatible
   - Function arguments match parameter types
   - Return types match function declarations

2. **Scope Checking**
   - Variables are declared before use
   - No duplicate declarations in same scope
   - Functions exist when called

3. **Array Validation**
   - Index expressions are integers
   - Static bounds checking (when possible)

4. **Record Field Validation**
   - Field exists in record type
   - Field access on record-typed values

5. **Control Flow Validation**
   - Loop/if conditions are boolean

**Optimizations Applied:**
1. **Constant Folding** - Evaluate constant expressions at compile time
2. **If Simplification** - Replace `if true` and `if false` with branch bodies
3. **While False Elimination** - Remove loops with false conditions
4. **Dead Code Elimination** - Remove unused variable declarations
5. **Declaration Hoisting** - Move declarations from if branches to parent scope

### Phase 4: Code Generation (Java)

**Location:** `compiler/src/main/java/compiler/codegen/`

**Components:**
- `WasmCodeGenerator.java` - Main generator
- `CodeGenVisitor.java` - AST visitor
- `ASTVisitor.java` - Visitor interface
- `CodeGenSymbolTable.java` - Symbol table for codegen
- `MemoryLayout.java` - Memory management
- `WasmPrinter.java` - Output formatter
- Various utility classes

**Process:**
1. Traverse optimized AST from semantic analyzer
2. Maintain symbol table with WASM-specific information
3. Emit WebAssembly instructions for each AST node
4. Manage memory allocation for arrays/records
5. Generate complete WASM module with imports and exports

**Output:**
- WAT (WebAssembly Text Format)
- Optionally: WASM binary format

## Data Flow Between Phases

### Lexer → Parser

```
Lexer              Parser
─────              ──────
Token Stream  →   AST
                  Symbol Table
                  Errors
```

### Parser → Semantic Analyzer

```
Parser                      Semantic Analyzer
──────                      ──────────────────
AST               →        Optimized AST
Symbol Table      →        Enhanced Symbol Table
                           Errors/Warnings
```

### Semantic Analyzer → Code Generator

```
Semantic Analyzer           Code Generator
─────────────────           ──────────────
Optimized AST       →      WASM Module
Enhanced Symbol Table →    Memory Layout Info
Type Information    →      WASM Instructions
```

## Error Handling Strategy

### Lexer Errors
- **Position Tracking** - Line and column numbers
- **Recovery** - Skip to next token
- **Examples** - Unclosed strings, invalid characters

### Parser Errors
- **Error Messages** - Grammar violations
- **Recovery** - Skip to synchronization points
- **Examples** - Missing semicolon, unexpected token

### Semantic Errors
- **Type Errors** - Type mismatch in assignment
- **Scope Errors** - Undefined variable, duplicate declaration
- **Examples** - Real assigned to boolean, function called with wrong arity

### Code Generation Errors
- **Internal Errors** - Should not happen if earlier phases correct
- **Memory Issues** - Allocation failures
- **Examples** - Stack overflow, invalid memory access

## Complete Example

### Source Code (test.i)

```
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end

var result: integer is factorial(5);
print result;
```

### Phase 1: Lexer Output

```
[ROUTINE] [IDENTIFIER(factorial)] 
[LPAREN] [IDENTIFIER(n)] [COLON] [INTEGER] [RPAREN]
[COLON] [INTEGER] [IS]
...
[EOF]
```

### Phase 2: Parser Output (Simplified AST)

```
ProgramNode
├── DeclarationNode
│   └── RoutineDeclarationNode
│       ├── RoutineHeaderNode
│       │   ├── name: "factorial"
│       │   └── parameters: [(n, integer)]
│       │   └── returnType: integer
│       └── RoutineBodyNode
│           └── IfStatementNode
│               ├── condition: n <= 1
│               ├── thenBranch: return 1
│               └── elseBranch: return n * factorial(n-1)
└── VariableDeclarationNode
    ├── name: "result"
    ├── type: integer
    └── initializer: factorial(5)
```

### Phase 3: Semantic Analyzer Output

```
✓ Type checking passed
✓ Scope checking passed
✓ Function definitions valid
✓ All optimizations applied

AST is unchanged (no constant folding opportunities in this example)
```

### Phase 4: Code Generator Output (WAT)

```wasm
(module
  (import "wasi_snapshot_preview1" "fd_write" 
    (func $fd_write (param i32 i32 i32 i32) (result i32)))
  
  (memory 1)
  (export "memory" (memory 0))
  
  (global $heap_ptr (mut i32) (i32.const 0x1000))
  
  (func $alloc (param $size i32) (result i32)
    ; ... memory allocation function ...
  )
  
  (func $factorial (param $n i32) (result i32)
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
  
  (func $print_int (param $value i32)
    ; ... print implementation ...
  )
  
  (func $_start
    i32.const 5
    call $factorial
    local.set $result
    
    local.get $result
    call $print_int
  )
  
  (export "_start" (func $_start))
)
```

## Testing Integration

### Unit Tests

```bash
# Test Java lexer
./gradlew :tests:test

# Test C++ parser
cd compiler/src/main/cpp/parser
./run_tests.sh
```

### Integration Tests

```bash
# Full compilation pipeline
./gradlew build
java -cp compiler/build/libs/* compiler.Compiler tests/test.i -o /tmp/test.wat
cat /tmp/test.wat
```

### Docker Testing

```bash
# Complete testing in Docker container
bash ./docker_test.sh
```

## Compiler Invocation

### Command Line Usage

```bash
# Basic usage
java compiler.Compiler source.i

# Specify output file
java compiler.Compiler source.i -o output.wat

# Help
java compiler.Compiler --help
```

### Exit Codes

- `0` - Success
- `1` - Lexer/Parser/Analyzer/Codegen error
- `2` - File not found
- `3` - I/O error

## Compilation Modes

### Debug Mode

```bash
# Enable debug output
export DEBUG_CODEGEN=1
java compiler.Compiler test.i -o test.wat
```

### Verbose Mode

```bash
# Show all phases
java -Dverbose=true compiler.Compiler test.i
```

### Optimization Levels

```
-O0  - No optimization (debugging)
-O1  - Basic optimization
-O2  - Aggressive optimization
```

## Performance Characteristics

| Phase | Time | Memory | Notes |
|-------|------|--------|-------|
| Lexer | O(n) | O(tokens) | Fast, linear scan |
| Parser | O(n) | O(AST size) | Bison optimized |
| Analyzer | O(n) | O(AST size) | Single pass |
| Codegen | O(n) | O(WAT size) | Linear traversal |

Where n = source code size

## Troubleshooting

### Issue: Lexer not found
```
Solution: Rebuild with ./gradlew build
```

### Issue: Parser compilation fails
```
Solution: Check JAVA_HOME is set correctly
          export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
          cd compiler/src/main/cpp/parser && make clean && make
```

### Issue: JNI library not loading
```
Solution: Ensure libparser.so is in LD_LIBRARY_PATH
          export LD_LIBRARY_PATH=compiler/src/main/cpp/parser:$LD_LIBRARY_PATH
```

### Issue: Type mismatch error
```
Solution: Check your variable declarations and assignments
          Example: var x: boolean is 3.14; -> ERROR
                   var x: real is 3.14;    -> OK
```

## References

1. **Project Documentation**
   - `docs/lexer_scope.md` - Lexer specification
   - `docs/parser_scope.md` - Parser grammar and rules
   - `docs/analyzer.md` - Semantic analyzer details
   - `docs/codegen_scope.md` - Code generation specification
   - `docs/codegen_implementation.md` - Implementation guide

2. **External References**
   - WebAssembly: https://webassembly.org/
   - Bison: https://www.gnu.org/software/bison/
   - Flex: https://github.com/westes/flex
   - WABT: https://github.com/WebAssembly/wabt

3. **Compiler Theory**
   - "Compilers: Principles, Techniques, and Tools" (Dragon Book)
   - "Engineering a Compiler" (Torczon & Cooper)

