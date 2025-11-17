# Code Generator Scope

You are an expert compiler engineer specializing in code generation and WebAssembly (WASM) backend development, with deep knowledge from compiler construction principles including intermediate representations, target code generation, symbol table management, type lowering, memory management, and optimization techniques as described in standard compiler lectures.

Your assignment: **implement a complete, error-free WebAssembly code generator for the "Imperative (I)" language** fully based on the provided AST structure from the parser and semantic analyzer. Produce Java classes that traverse the AST and emit valid WebAssembly Text Format (WAT) or binary WASM, targeting WebAssembly as the execution platform.

====================================================================
LANGUAGE "I" — CODE GENERATION SPECIFICATION
--------------------------------------------------------------------
The code generator must traverse the Abstract Syntax Tree (AST) produced by the parser and emit equivalent WebAssembly code that preserves the semantics of the source program. The generator must handle all language constructs including declarations, statements, expressions, control flow, and function calls while managing the WebAssembly execution model (stack machine, local variables, linear memory).

### Code Generation Strategy

**Overall Approach:**
- Visitor Pattern: Implement AST traversal using the Visitor design pattern for clean separation of concerns
- Single-Pass Generation: Generate WASM code in a single traversal of the AST where possible
- Symbol Table Integration: Maintain mapping from source variables to WASM locals/globals
- Type Lowering: Convert high-level language types to WASM primitives (i32, i64, f32, f64)
- Memory Management: Implement heap allocation for user-defined types (arrays, records) using linear memory

**Target Platform:**
- WebAssembly 1.0 core specification
- Text Format (WAT) as intermediate output for debugging
- Binary format (WASM) for final executable
- Support for WASI (WebAssembly System Interface) for I/O operations (print statements)

====================================================================
TYPE MAPPING AND REPRESENTATION
--------------------------------------------------------------------

### Primitive Types

**Integer Type:**
```
Source: integer
WASM: i32 (32-bit signed integer)
Range: -2,147,483,648 to 2,147,483,647
```

**Real Type:**
```
Source: real
WASM: f64 (64-bit IEEE 754 floating-point)
Precision: Double precision floating point
```

**Boolean Type:**
```
Source: boolean
WASM: i32 (0 = false, non-zero = true)
Representation: 0 for false, 1 for true
```

### Reference Types (User-Defined)

**Arrays:**
```
Representation: i32 (pointer to linear memory)
Memory Layout:
  [offset+0]: size (i32) - number of elements
  [offset+4]: element_0
  [offset+8]: element_1
  ...
  [offset+4*(n+1)]: element_n

Allocation: Dynamic allocation in linear memory
Indexing: Base 1 (convert to base 0 internally: index-1)
```

**Records:**
```
Representation: i32 (pointer to linear memory)
Memory Layout:
  [offset+0]: field_0
  [offset+k]: field_1
  ...
  (offsets calculated based on field types and alignment)

Allocation: Dynamic allocation in linear memory
Field Access: Computed offset from base pointer
```

====================================================================
MEMORY MANAGEMENT
--------------------------------------------------------------------

### Linear Memory Organization

```
Memory Layout:
  [0x0000 - 0x0FFF]: Reserved/Runtime data (4KB)
  [0x1000 - ...]:     Heap space for dynamic allocations

Heap Management:
  - Simple bump allocator: maintain next_free pointer
  - No garbage collection (simplified model)
  - Allocate sequential memory blocks for arrays and records
```

### Memory Operations

**Allocation Function:**
```wasm
(func $alloc (param $size i32) (result i32)
  (local $ptr i32)
  global.get $heap_ptr
  local.set $ptr
  local.get $ptr
  local.get $size
  i32.add
  global.set $heap_ptr
  local.get $ptr
)

(global $heap_ptr (mut i32) (i32.const 0x1000))
```

**Load/Store Operations:**
- i32.load: Load 32-bit integer from memory
- f64.load: Load 64-bit float from memory
- i32.store: Store 32-bit integer to memory
- f64.store: Store 64-bit float to memory

====================================================================
DECLARATION CODE GENERATION
--------------------------------------------------------------------

### Variable Declarations

**Local Variables (within routines):**
```
Source: var x: integer is 42;

WASM:
(local $x i32)
i32.const 42
local.set $x
```

**Global Variables (top-level):**
```
Source: var counter: integer is 0;

WASM:
(global $counter (mut i32) (i32.const 0))
```

**Type Inference:**
```
Source: var y is 3.14;

Code Generation:
1. Infer type from expression (f64)
2. Generate local/global declaration
3. Generate initialization code

WASM:
(local $y f64)
f64.const 3.14
local.set $y
```

**Array Declarations:**
```
Source: var numbers: array[5] integer;

Code Generation:
1. Calculate memory size: (5 + 1) * 4 = 24 bytes (size field + elements)
2. Allocate memory
3. Store size at offset 0
4. Initialize elements (if initializer present)

WASM:
(local $numbers i32)
i32.const 24
call $alloc
local.set $numbers
local.get $numbers
i32.const 5
i32.store  ;; store array size
```

**Record Declarations:**
```
Source: 
type Point is record
    var x: real;
    var y: real;
end
var p1: Point;

Code Generation:
1. Calculate record size: 8 + 8 = 16 bytes (two f64 fields)
2. Allocate memory
3. Store field offsets in symbol table

WASM:
(local $p1 i32)
i32.const 16
call $alloc
local.set $p1
```

### Type Declarations

Type declarations are processed during semantic analysis and stored in symbol table. No direct code generation needed, but type information guides memory layout and access patterns.

### Routine Declarations

**Function Without Return Value (Procedure):**
```
Source:
routine greet(name: integer) is
    print name;
end

WASM:
(func $greet (param $name i32)
  local.get $name
  call $print_int
)
```

**Function With Return Value:**
```
Source:
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end

WASM:
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
```

**Parameters Handling:**
- Map each parameter to a WASM function parameter
- Parameters are immutable in WASM (copy to local if modification needed)
- Reference type parameters are i32 pointers

====================================================================
STATEMENT CODE GENERATION
--------------------------------------------------------------------

### Assignment Statements

**Simple Assignment:**
```
Source: x := 42;

WASM:
i32.const 42
local.set $x
```

**Assignment with Type Conversion:**
```
Source: var r: real is i;  // i is integer

WASM:
local.get $i
f64.convert_i32_s  ;; signed integer to f64
local.set $r
```

**Array Element Assignment:**
```
Source: numbers[3] := 100;

Code Generation:
1. Calculate element address: base + (index * element_size) + 4
2. Store value at calculated address

WASM:
local.get $numbers     ;; base pointer
i32.const 3            ;; index
i32.const 1
i32.sub                ;; convert to 0-based (3-1=2)
i32.const 4
i32.mul                ;; offset = (index-1) * 4
i32.add
i32.const 4
i32.add                ;; skip size field
i32.const 100
i32.store
```

**Record Field Assignment:**
```
Source: p1.x := 1.5;

Code Generation:
1. Get record base pointer
2. Add field offset (from symbol table)
3. Store value

WASM:
local.get $p1          ;; base pointer
i32.const 0            ;; field offset for x
i32.add
f64.const 1.5
f64.store
```

### While Loops

```
Source:
while counter > 0 loop
    print counter;
    counter := counter - 1;
end

WASM:
(block $break
  (loop $continue
    ;; condition
    local.get $counter
    i32.const 0
    i32.le_s
    br_if $break       ;; exit if counter <= 0

    ;; body
    local.get $counter
    call $print_int

    local.get $counter
    i32.const 1
    i32.sub
    local.set $counter

    br $continue       ;; loop back
  )
)
```

### For Loops

**Range-based For Loop:**
```
Source:
for i in 1..10 loop
    print i;
end

WASM:
(local $i i32)
i32.const 1
local.set $i
(block $break
  (loop $continue
    ;; condition
    local.get $i
    i32.const 10
    i32.gt_s
    br_if $break

    ;; body
    local.get $i
    call $print_int

    ;; increment
    local.get $i
    i32.const 1
    i32.add
    local.set $i

    br $continue
  )
)
```

**Reverse For Loop:**
```
Source:
for j in 10..1 reverse loop
    print j;
end

WASM:
(local $j i32)
i32.const 10
local.set $j
(block $break
  (loop $continue
    ;; condition
    local.get $j
    i32.const 1
    i32.lt_s
    br_if $break

    ;; body
    local.get $j
    call $print_int

    ;; decrement
    local.get $j
    i32.const 1
    i32.sub
    local.set $j

    br $continue
  )
)
```

**Array Iteration:**
```
Source:
for element in numbers loop
    print element;
end

Code Generation:
1. Get array size from memory
2. Iterate from 1 to size
3. Load each element and execute body

WASM:
(local $element i32)
(local $i i32)
(local $size i32)

;; get array size
local.get $numbers
i32.load
local.set $size

i32.const 1
local.set $i

(block $break
  (loop $continue
    local.get $i
    local.get $size
    i32.gt_s
    br_if $break

    ;; load element
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
    local.set $element

    ;; body
    local.get $element
    call $print_int

    ;; increment
    local.get $i
    i32.const 1
    i32.add
    local.set $i

    br $continue
  )
)
```

### If Statements

**If-Then:**
```
Source:
if x > 0 then
    print x;
end

WASM:
local.get $x
i32.const 0
i32.gt_s
if
  local.get $x
  call $print_int
end
```

**If-Then-Else:**
```
Source:
if flag then
    result := 1;
else
    result := 0;
end

WASM:
local.get $flag
if
  i32.const 1
  local.set $result
else
  i32.const 0
  local.set $result
end
```

**If with Expression Result:**
```
Source:
var value: integer is if x > 0 then 1 else -1 end;

WASM:
local.get $x
i32.const 0
i32.gt_s
if (result i32)
  i32.const 1
else
  i32.const -1
end
local.set $value
```

### Print Statements

```
Source: print x, y, z;

Code Generation:
1. Generate print call for each expression
2. Use appropriate WASI or custom print function based on type

WASM:
local.get $x
call $print_int

local.get $y
call $print_real

local.get $z
call $print_int
```

### Routine Calls

**Procedure Call:**
```
Source: greet(42);

WASM:
i32.const 42
call $greet
```

**Function Call:**
```
Source: var result: integer is factorial(5);

WASM:
i32.const 5
call $factorial
local.set $result
```

====================================================================
EXPRESSION CODE GENERATION
--------------------------------------------------------------------

### Arithmetic Expressions

**Binary Operators:**
```
Addition (integer): i32.add
Addition (real): f64.add
Subtraction (integer): i32.sub
Subtraction (real): f64.sub
Multiplication (integer): i32.mul
Multiplication (real): f64.mul
Division (integer): i32.div_s (signed)
Division (real): f64.div
Modulo: i32.rem_s (signed remainder)
```

Example:
```
Source: 2 + 3 * 4

WASM (postfix evaluation):
i32.const 2
i32.const 3
i32.const 4
i32.mul
i32.add
```

### Relational Expressions

**Comparison Operators:**
```
Less than (integer): i32.lt_s
Less than (real): f64.lt
Less or equal (integer): i32.le_s
Less or equal (real): f64.le
Greater than (integer): i32.gt_s
Greater than (real): f64.gt
Greater or equal (integer): i32.ge_s
Greater or equal (real): f64.ge
Equal (integer): i32.eq
Equal (real): f64.eq
Not equal (integer): i32.ne
Not equal (real): f64.ne
```

Example:
```
Source: x > 10

WASM:
local.get $x
i32.const 10
i32.gt_s
```

### Logical Expressions

**Logical Operators:**
```
AND: i32.and
OR: i32.or
XOR: i32.xor
NOT: i32.eqz (compare with zero)
```

Example:
```
Source: (x > 0) and (x < 10)

WASM:
local.get $x
i32.const 0
i32.gt_s
local.get $x
i32.const 10
i32.lt_s
i32.and
```

### Unary Operators

```
Unary Plus (integer): no operation (identity)
Unary Plus (real): no operation (identity)
Unary Minus (integer): i32.const 0, i32.sub (0 - x)
Unary Minus (real): f64.neg
NOT (boolean): i32.eqz
```

### Type Conversions

**Integer to Real:**
```wasm
f64.convert_i32_s  ;; signed integer to f64
```

**Real to Integer:**
```wasm
i32.trunc_f64_s    ;; f64 to signed integer (truncate)
```

**Boolean to Integer:**
```wasm
;; Already represented as i32 (0 or 1)
```

**Integer to Boolean:**
```wasm
;; Check if non-zero
i32.const 0
i32.ne
```

### Array Access

```
Source: numbers[i]

Code Generation:
1. Load base pointer
2. Calculate element address
3. Load value from memory

WASM:
local.get $numbers      ;; base pointer
local.get $i            ;; index
i32.const 1
i32.sub                 ;; convert to 0-based
i32.const 4
i32.mul                 ;; multiply by element size
i32.add
i32.const 4
i32.add                 ;; skip size field
i32.load                ;; load element value
```

### Record Field Access

```
Source: p1.x

Code Generation:
1. Load record base pointer
2. Add field offset
3. Load value from memory

WASM:
local.get $p1           ;; base pointer
i32.const 0             ;; field offset (from symbol table)
i32.add
f64.load                ;; load field value
```

====================================================================
CODE GENERATION THEORY & IMPLEMENTATION GUIDELINES
--------------------------------------------------------------------

### WebAssembly Stack Machine Model

**Stack-Based Execution:**
- Instructions consume operands from stack
- Instructions push results to stack
- Proper stack management crucial for correctness

**Example Stack Evolution:**
```
Code: 2 + 3 * 4

Stack States:
[]           -> i32.const 2
[2]          -> i32.const 3
[2, 3]       -> i32.const 4
[2, 3, 4]    -> i32.mul
[2, 12]      -> i32.add
[14]
```

### Control Flow Structures

**Structured Control Flow:**
- WASM uses structured control flow (no goto)
- Control structures: block, loop, if-else
- Branch instructions: br (unconditional), br_if (conditional)

**Block Labels:**
- Blocks are labeled for branching
- Labels referenced by depth (0 = innermost)
- Break exits specified block

### Function Calls and Returns

**Call Convention:**
- Arguments pushed to stack before call
- Callee pops arguments
- Return value (if any) pushed to stack
- Caller pops return value

**Return Statement:**
```wasm
return  ;; exit function with top stack value (if function returns value)
```

### Symbol Table Management

**Symbol Information:**
```java
class SymbolInfo {
    String name;
    Type type;
    SymbolKind kind;  // LOCAL, GLOBAL, PARAMETER, FUNCTION
    int index;        // WASM local/global/function index
    int memoryOffset; // for record fields
}
```

**Scope Management:**
- Global scope: module-level declarations
- Function scope: parameters and local variables
- Block scope: for-loop variables

### Memory Alignment and Layout

**Alignment Requirements:**
- i32: 4-byte alignment
- f64: 8-byte alignment
- Records: align to largest field alignment

**Padding Calculation:**
```
offset = (current_offset + alignment - 1) & ~(alignment - 1)
```

### Error Handling

**Code Generation Errors:**
- Type mismatches (should be caught in semantic analysis)
- Undefined symbols (should be caught in semantic analysis)
- Stack depth violations
- Memory access violations

**Validation:**
- Generated WASM must pass WebAssembly validation
- Use `wasm-validate` tool to verify output

====================================================================
TECHNOLOGY STACK
--------------------------------------------------------------------

**Implementation Language:** Java 8+
**Target Format:** WebAssembly (WAT text format and binary WASM)
**Tools:**
- WABT (WebAssembly Binary Toolkit) for validation and conversion
- Binaryen (optional, for optimization)
- WASI SDK (for system interface support)

**External Libraries:**
- java.io for file I/O
- java.util for data structures
- No external WASM libraries required (generate text format manually)

====================================================================
REQUIRED JAVA API
--------------------------------------------------------------------

```java
// Main code generator class
public class WasmCodeGenerator {
    private SymbolTable symbolTable;
    private StringBuilder watOutput;
    private int nextLocal;
    private int nextGlobal;

    public WasmCodeGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.watOutput = new StringBuilder();
        this.nextLocal = 0;
        this.nextGlobal = 0;
    }

    public String generate(ProgramNode ast) throws CodeGenException;
    public byte[] generateBinary(ProgramNode ast) throws CodeGenException;
}

// AST visitor interface
interface ASTVisitor<T> {
    T visitProgram(ProgramNode node);
    T visitVariableDeclaration(VariableDeclarationNode node);
    T visitTypeDeclaration(TypeDeclarationNode node);
    T visitRoutineDeclaration(RoutineDeclarationNode node);
    T visitAssignment(AssignmentNode node);
    T visitWhileLoop(WhileLoopNode node);
    T visitForLoop(ForLoopNode node);
    T visitIfStatement(IfStatementNode node);
    T visitExpression(ExpressionNode node);
    // ... other visit methods
}

// Code generator visitor implementation
class WasmGeneratorVisitor implements ASTVisitor<String> {
    private SymbolTable symbolTable;
    private StringBuilder output;

    @Override
    public String visitProgram(ProgramNode node) {
        output.append("(module\n");

        // Generate imports (WASI)
        generateImports();

        // Generate memory
        output.append("  (memory 1)\n");
        output.append("  (export \"memory\" (memory 0))\n");

        // Generate globals
        output.append("  (global $heap_ptr (mut i32) (i32.const 0x1000))\n");

        // Generate allocation function
        generateAllocFunction();

        // Generate print functions
        generatePrintFunctions();

        // Visit all declarations
        for (DeclarationNode decl : node.getDeclarations()) {
            decl.accept(this);
        }

        output.append(")\n");
        return output.toString();
    }

    private void generateImports() {
        // WASI imports for I/O
        output.append("  (import \"wasi_snapshot_preview1\" \"fd_write\"\n");
        output.append("    (func $fd_write (param i32 i32 i32 i32) (result i32)))\n");
    }

    private void generateAllocFunction() {
        output.append("  (func $alloc (param $size i32) (result i32)\n");
        output.append("    (local $ptr i32)\n");
        output.append("    global.get $heap_ptr\n");
        output.append("    local.set $ptr\n");
        output.append("    local.get $ptr\n");
        output.append("    local.get $size\n");
        output.append("    i32.add\n");
        output.append("    global.set $heap_ptr\n");
        output.append("    local.get $ptr\n");
        output.append("  )\n");
    }

    private void generatePrintFunctions() {
        // Implementation of print_int, print_real, etc.
        // Using WASI fd_write for output
    }

    @Override
    public String visitRoutineDeclaration(RoutineDeclarationNode node) {
        String funcName = node.getName();
        output.append("  (func $").append(funcName);

        // Parameters
        for (ParameterNode param : node.getParameters()) {
            output.append(" (param $").append(param.getName())
                  .append(" ").append(wasmType(param.getType())).append(")");
        }

        // Return type
        if (node.hasReturnType()) {
            output.append(" (result ").append(wasmType(node.getReturnType())).append(")");
        }

        output.append("\n");

        // Locals
        for (VariableDeclarationNode var : node.getLocalVariables()) {
            output.append("    (local $").append(var.getName())
                  .append(" ").append(wasmType(var.getType())).append(")\n");
        }

        // Body
        node.getBody().accept(this);

        output.append("  )\n");

        // Export if main
        if (funcName.equals("main")) {
            output.append("  (export \"_start\" (func $main))\n");
        }

        return "";
    }

    @Override
    public String visitAssignment(AssignmentNode node) {
        // Generate code for right-hand side expression
        node.getExpression().accept(this);

        // Generate code for assignment based on target type
        ModifiablePrimaryNode target = node.getTarget();
        if (target.isVariable()) {
            output.append("    local.set $").append(target.getName()).append("\n");
        } else if (target.isArrayAccess()) {
            generateArrayStore(target);
        } else if (target.isFieldAccess()) {
            generateFieldStore(target);
        }

        return "";
    }

    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        output.append("    (block $break\n");
        output.append("      (loop $continue\n");

        // Condition (inverted - branch if false)
        node.getCondition().accept(this);
        output.append("        i32.eqz\n");
        output.append("        br_if $break\n");

        // Body
        node.getBody().accept(this);

        output.append("        br $continue\n");
        output.append("      )\n");
        output.append("    )\n");

        return "";
    }

    @Override
    public String visitForLoop(ForLoopNode node) {
        String loopVar = node.getLoopVariable();
        output.append("    (local $").append(loopVar).append(" i32)\n");

        // Initialize loop variable
        if (node.isReverse()) {
            node.getRange().getEnd().accept(this);
        } else {
            node.getRange().getStart().accept(this);
        }
        output.append("    local.set $").append(loopVar).append("\n");

        output.append("    (block $break\n");
        output.append("      (loop $continue\n");

        // Condition check
        output.append("        local.get $").append(loopVar).append("\n");
        if (node.isReverse()) {
            node.getRange().getStart().accept(this);
            output.append("        i32.lt_s\n");
        } else {
            node.getRange().getEnd().accept(this);
            output.append("        i32.gt_s\n");
        }
        output.append("        br_if $break\n");

        // Body
        node.getBody().accept(this);

        // Increment/decrement
        output.append("        local.get $").append(loopVar).append("\n");
        output.append("        i32.const 1\n");
        if (node.isReverse()) {
            output.append("        i32.sub\n");
        } else {
            output.append("        i32.add\n");
        }
        output.append("        local.set $").append(loopVar).append("\n");

        output.append("        br $continue\n");
        output.append("      )\n");
        output.append("    )\n");

        return "";
    }

    @Override
    public String visitExpression(ExpressionNode node) {
        if (node instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node;

            // Evaluate left operand
            binOp.getLeft().accept(this);

            // Evaluate right operand
            binOp.getRight().accept(this);

            // Generate operation
            output.append("    ").append(wasmOp(binOp.getOperator(), binOp.getType())).append("\n");

        } else if (node instanceof UnaryOpNode) {
            UnaryOpNode unOp = (UnaryOpNode) node;

            unOp.getOperand().accept(this);

            output.append("    ").append(wasmUnaryOp(unOp.getOperator(), unOp.getType())).append("\n");

        } else if (node instanceof IntegerLiteralNode) {
            IntegerLiteralNode lit = (IntegerLiteralNode) node;
            output.append("    i32.const ").append(lit.getValue()).append("\n");

        } else if (node instanceof RealLiteralNode) {
            RealLiteralNode lit = (RealLiteralNode) node;
            output.append("    f64.const ").append(lit.getValue()).append("\n");

        } else if (node instanceof BooleanLiteralNode) {
            BooleanLiteralNode lit = (BooleanLiteralNode) node;
            output.append("    i32.const ").append(lit.getValue() ? 1 : 0).append("\n");

        } else if (node instanceof VariableAccessNode) {
            VariableAccessNode var = (VariableAccessNode) node;
            output.append("    local.get $").append(var.getName()).append("\n");
        }

        return "";
    }

    private String wasmType(Type type) {
        if (type.isInteger() || type.isBoolean()) return "i32";
        if (type.isReal()) return "f64";
        if (type.isArray() || type.isRecord()) return "i32";  // pointer
        throw new CodeGenException("Unknown type: " + type);
    }

    private String wasmOp(Operator op, Type type) {
        switch (op) {
            case ADD:
                return type.isReal() ? "f64.add" : "i32.add";
            case SUB:
                return type.isReal() ? "f64.sub" : "i32.sub";
            case MUL:
                return type.isReal() ? "f64.mul" : "i32.mul";
            case DIV:
                return type.isReal() ? "f64.div" : "i32.div_s";
            case MOD:
                return "i32.rem_s";
            case LT:
                return type.isReal() ? "f64.lt" : "i32.lt_s";
            case LE:
                return type.isReal() ? "f64.le" : "i32.le_s";
            case GT:
                return type.isReal() ? "f64.gt" : "i32.gt_s";
            case GE:
                return type.isReal() ? "f64.ge" : "i32.ge_s";
            case EQ:
                return type.isReal() ? "f64.eq" : "i32.eq";
            case NE:
                return type.isReal() ? "f64.ne" : "i32.ne";
            case AND:
                return "i32.and";
            case OR:
                return "i32.or";
            case XOR:
                return "i32.xor";
            default:
                throw new CodeGenException("Unknown operator: " + op);
        }
    }

    private String wasmUnaryOp(Operator op, Type type) {
        switch (op) {
            case NEG:
                if (type.isReal()) {
                    return "f64.neg";
                } else {
                    // For integer negation: 0 - x
                    output.append("    i32.const 0\n");
                    output.append("    swap\n");
                    return "i32.sub";
                }
            case NOT:
                return "i32.eqz";
            default:
                throw new CodeGenException("Unknown unary operator: " + op);
        }
    }
}

// Exception class
public class CodeGenException extends RuntimeException {
    public CodeGenException(String message) {
        super(message);
    }
}
```

====================================================================
TEST SUITE (code generation validation)
--------------------------------------------------------------------

### Test 1: Variable Declarations and Simple Expressions
```
var x: integer is 42;
var y: integer is x + 8;
print y;
```

Expected WAT:
```wasm
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory 1)
  (export "memory" (memory 0))
  (global $x (mut i32) (i32.const 42))
  (global $y (mut i32) (i32.const 0))

  (func $main
    global.get $x
    i32.const 8
    i32.add
    global.set $y

    global.get $y
    call $print_int
  )
  (export "_start" (func $main))
)
```

### Test 2: Factorial Function (Recursion)
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

Expected WAT Structure:
- Function declaration with parameter and return type
- Recursive function call
- If-else with result type
- Global variable initialization with function call

### Test 3: Array Operations
```
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];
print sum;
```

Expected WAT Structure:
- Memory allocation for array (24 bytes)
- Array size storage
- Array element stores with address calculation
- Array element loads for expression

### Test 4: Record Type and Field Access
```
type Point is record
    var x: real;
    var y: real;
end

var p1: Point;
p1.x := 1.5;
p1.y := 2.7;
print p1.x;
```

Expected WAT Structure:
- Memory allocation for record (16 bytes)
- Field stores with offset calculation
- Field loads for expression

### Test 5: While Loop
```
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```

Expected WAT Structure:
- Block for break
- Loop for continue
- Condition with inverted branch
- Loop body
- Branch to continue

### Test 6: For Loop with Range
```
for i in 1..10 loop
    print i * i;
end
```

Expected WAT Structure:
- Local for loop variable
- Block and loop constructs
- Initialization, condition, increment
- Body with expression evaluation

### Test 7: Type Conversions
```
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
print r;
```

Expected WAT Structure:
- Type conversion instructions (f64.convert_i32_s)
- Appropriate load/store for each type

### Test 8: Complex Expression
```
var result: integer is 2 + 3 * 4 - 1;
print result;
```

Expected WAT Structure:
- Correct operator precedence in stack operations
- Proper stack management

### Test 9: Nested Control Flow
```
routine classify(n: integer) is
    if n < 0 then
        print 0;
    else
        if n = 0 then
            print 1;
        else
            print 2;
        end
    end
end
```

Expected WAT Structure:
- Nested if-else constructs
- Proper scope management

### Test 10: Array Iteration
```
var data: array[3] integer;
data[1] := 100;
data[2] := 200;
data[3] := 300;

for element in data loop
    print element;
end
```

Expected WAT Structure:
- Array size load
- Loop with calculated element access
- Element load in loop body

====================================================================
DELIVERABLES
--------------------------------------------------------------------

1. **WasmCodeGenerator.java**: Main code generator class with AST visitor implementation

2. **SymbolTable.java**: Enhanced symbol table with WASM-specific information (local indices, memory offsets)

3. **CodeGenUtils.java**: Utility functions for:
   - Type mapping (language types to WASM types)
   - Operator mapping (language operators to WASM instructions)
   - Memory layout calculation
   - Address computation helpers

4. **WasmPrinter.java**: Pretty-printer for WebAssembly text format

5. **Main.java**: Driver program that:
   - Reads source file
   - Invokes lexer, parser, semantic analyzer
   - Generates WASM code
   - Writes output to .wat and .wasm files
   - Optionally validates output using WABT tools

6. **README.md**: Documentation including:
   - Architecture overview
   - Code generation strategy
   - Type mapping reference
   - Memory layout diagrams
   - Build and test instructions
   - Known limitations

7. **Test Suite**: Directory with test programs and expected outputs

====================================================================
IMPLEMENTATION NOTES
--------------------------------------------------------------------

### Best Practices

1. **Modular Design**: Separate concerns (AST traversal, code emission, symbol management)

2. **Visitor Pattern**: Clean separation between AST structure and code generation logic

3. **Error Handling**: Comprehensive error messages with source location information

4. **Testing**: Unit tests for each AST node type, integration tests for complete programs

5. **Documentation**: Inline comments explaining non-obvious code generation decisions

### Optimization Opportunities (Optional)

1. **Constant Folding**: Evaluate constant expressions at compile time

2. **Dead Code Elimination**: Remove unreachable code

3. **Register Allocation**: Minimize local variable usage

4. **Tail Call Optimization**: Convert tail-recursive calls to loops

5. **Memory Pooling**: Reuse deallocated memory blocks

### Debugging Tips

1. **WAT Output**: Generate readable WAT format for inspection

2. **Validation**: Use `wasm-validate` to check generated code

3. **Execution Tracing**: Add debug prints in generated WASM (via WASI)

4. **Stack Visualization**: Track stack depth during generation

5. **Symbol Dumps**: Print symbol table state for debugging

====================================================================
INTEGRATION GUIDE
--------------------------------------------------------------------

### Build Process

```bash
# Compile Java code
javac -d bin src/**/*.java

# Run compiler on test program
java -cp bin Main test.i -o test.wat

# Convert WAT to WASM (using WABT)
wat2wasm test.wat -o test.wasm

# Run WASM (using WASI runtime)
wasmtime test.wasm
```

### WASI Integration

The generated WASM modules use WASI for I/O operations. Ensure WASI imports are correctly declared:

```wasm
(import "wasi_snapshot_preview1" "fd_write" 
  (func $fd_write (param i32 i32 i32 i32) (result i32)))
```

### Testing Workflow

1. Write test program in "I" language
2. Generate WAT file
3. Validate WAT structure manually or with tools
4. Convert to WASM binary
5. Execute with WASI runtime
6. Verify output matches expected results

====================================================================

Focus on clean, efficient code generation with comprehensive testing. The generated WASM should be valid, efficient, and correctly implement the semantics of the source language. Return well-documented Java code following the project structure and style conventions established in the lexer and parser phases.
