# Parser Documentation

## Overview

The parser component of the compiler implements a recursive descent parser that converts tokens from the lexer into an Abstract Syntax Tree (AST) representation of the source code. It supports the full grammar of the imperative programming language as specified in the project requirements.

## Architecture

### Core Classes

#### Parser
The main parser class that orchestrates the parsing process.

#### AST Node Hierarchy
- `AstNode`: Base class for all AST nodes
- Expression nodes: `BinaryOpNode`, `UnaryOpNode`, `IntegerLiteralNode`, etc.
- Statement nodes: `AssignmentNode`, `WhileLoopNode`, `IfStatementNode`, etc.
- Declaration nodes: `VariableDeclarationNode`, `TypeDeclarationNode`, etc.

#### Exceptions
- `ParserException`: Thrown when parsing errors occur

## Usage

### Basic Usage

```java
import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ProgramNode;
import compiler.parser.ParserException;
import compiler.lexer.LexerException;

import java.io.StringReader;

// Create lexer with source code
Lexer lexer = new Lexer(new StringReader("var x: integer is 42;"));

// Create parser
Parser parser = new Parser(lexer);

// Parse program
try {
    ProgramNode ast = parser.parseProgram();
    System.out.println("Parsing successful!");
    System.out.println(ast.toString());
} catch (ParserException e) {
    System.err.println("Parse error: " + e.getMessage());
} catch (LexerException e) {
    System.err.println("Lexical error: " + e.getMessage());
}
```

### AST Structure

The parser builds a hierarchical AST where each node represents a syntactic construct:

```
ProgramNode
├── declarations: List<AstNode>
    ├── VariableDeclarationNode
    │   ├── name: String
    │   ├── type: TypeNode
    │   └── initializer: ExpressionNode (optional)
    ├── TypeDeclarationNode
    │   ├── name: String
    │   └── type: TypeNode
    └── RoutineDeclarationNode
        ├── name: String
        ├── parameters: List<ParameterNode>
        ├── returnType: TypeNode (optional)
        └── body: List<AstNode> (optional)
```

### Supported Language Features

#### Declarations
- Variable declarations: `var x: integer;` or `var x: integer is 42;` or `var x is 42;`
- Type declarations: `type MyType is integer;`
- Routine declarations: `routine factorial(n: integer): integer is ... end`

#### Types
- Primitive types: `integer`, `real`, `boolean`
- Array types: `array[10] integer` or `array[] integer`
- Record types: `record var x: integer; var y: real; end`
- Type references: any user-defined type name

#### Statements
- Assignment: `x := 42;`
- Routine calls: `print(42);`
- While loops: `while condition loop ... end`
- For loops: `for i in 1..10 loop ... end` or `for i in 1..10 reverse loop ... end`
- If statements: `if condition then ... end` or `if condition then ... else ... end`
- Print statements: `print expr1, expr2, ...;`

#### Expressions
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `<`, `<=`, `>`, `>=`, `=`, `!=`
- Logical: `and`, `or`, `xor`, `not`
- Literals: integers, reals, booleans (`true`/`false`), strings
- Variables: `x`, `arr[5]`, `record.field`
- Routine calls: `factorial(5)`
- Parenthesized expressions: `(x + 1)`

### Error Handling

The parser provides detailed error messages with line and column information:

```java
try {
    ProgramNode ast = parser.parseProgram();
} catch (ParserException e) {
    System.err.printf("Parse error at line %d, column %d: %s%n",
                     e.getLine(), e.getColumn(), e.getMessage());
}
```

### Operator Precedence

The parser implements the following operator precedence (highest to lowest):

1. Primary expressions (literals, variables, calls)
2. Unary operators: `+`, `-`, `not`
3. Multiplicative: `*`, `/`, `%`
4. Additive: `+`, `-`
5. Relational: `<`, `<=`, `>`, `>=`, `=`, `!=`
6. Logical: `and`, `or`, `xor`

### Examples

#### Variable Declarations
```java
// Simple declaration
var x: integer;

// Declaration with initialization
var y: real is 3.14;

// Type-inferred declaration
var flag is true;
```

#### Array and Record Types
```java
// Array type declaration
type IntArray is array[100] integer;
var numbers: IntArray;

// Record type declaration
type Point is record
    var x: real;
    var y: real;
end;
var p: Point;
```

#### Control Structures
```java
// While loop
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end

// For loop
for i in 1..10 loop
    print i * i;
end

// For loop with reverse
for j in 10..1 reverse loop
    print j;
end

// If statement
if x > 0 then
    print "positive";
else
    print "non-positive";
end
```

#### Expressions
```java
// Arithmetic expressions
var result: integer is (2 + 3) * 4 - 1;

// Logical expressions
var condition: boolean is (x > 0) and not (y = 0);

// Array and record access
numbers[5] := 42;
point.x := 3.14;
point.y := 2.71;
```

#### Functions
```java
// Function declaration
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end

// Function call
var result: integer is factorial(5);
```

### AST Traversal

You can traverse the AST using the visitor pattern or by directly accessing node properties:

```java
public void printAST(ProgramNode program) {
    for (AstNode decl : program.getDeclarations()) {
        System.out.println(decl.toString());
    }
}

public void analyzeExpressions(AstNode node) {
    if (node instanceof BinaryOpNode binOp) {
        System.out.println("Binary operation: " + binOp.getOperator());
        analyzeExpressions(binOp.getLeft());
        analyzeExpressions(binOp.getRight());
    } else if (node instanceof VariableRefNode varRef) {
        System.out.println("Variable reference: " + varRef.getName());
    }
    // Handle other node types...
}
```

### Integration with Build System

The parser is built using Gradle. To compile and run tests:

```bash
# Build the project
./gradlew build

# Run parser tests
./gradlew test --tests "*TestParser*"

# Run all tests
./gradlew test
```

### Testing

The parser includes comprehensive tests covering:

- Variable declarations with different syntaxes
- Type declarations and user-defined types
- Array and record type handling
- Control structures (loops, conditionals)
- Expression parsing with proper precedence
- Error detection and reporting

Run the test suite to verify parser functionality:

```java
@Test
public void testComplexProgram() throws LexerException, ParserException {
    String sourceCode = """
        type Point is record
            var x: real;
            var y: real;
        end

        routine distance(p1: Point, p2: Point): real is
            var dx: real is p1.x - p2.x;
            var dy: real is p1.y - p2.y;
            return sqrt(dx * dx + dy * dy);
        end

        var origin: Point;
        var target: Point;
        origin.x := 0.0;
        origin.y := 0.0;
        target.x := 3.0;
        target.y := 4.0;

        var dist: real is distance(origin, target);
        print dist;
        """;

    ProgramNode program = parse(sourceCode);
    assertNotNull(program);
    // Additional assertions...
}
```

## Current Status

The parser implementation is **complete** and includes:

- ✅ Full AST node hierarchy for all language constructs
- ✅ Complete recursive descent parser implementation
- ✅ Proper operator precedence and associativity
- ✅ Comprehensive error handling with position information
- ✅ Full test suite covering all features
- ✅ Integration with existing lexer and build system

## Limitations

- The parser currently does not perform semantic analysis (type checking, scope resolution, etc.)
- Error recovery is limited; syntax errors typically halt parsing
- No optimization passes are implemented
- Code generation is not included in the parser component

## Future Extensions

The AST structure is designed to support additional compiler phases:

- **Semantic Analysis**: Type checking, scope resolution, symbol table management
- **Intermediate Code Generation**: Convert AST to intermediate representation
- **Optimization**: Constant folding, dead code elimination, etc.
- **Code Generation**: Generate target machine code or bytecode

Each AST node includes position information (line/column) to support detailed error reporting throughout the compilation pipeline.
