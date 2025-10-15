# Integration Guide

This guide explains how to integrate the Bison parser with the Java lexer and WASM code generator.

## JNI Bridge Setup for Java Lexer Communication

### 1. Java Side (Lexer Integration)

The Java lexer (in `compiler/src/main/java/compiler/lexer/`) provides tokens via JNI.

**TokenType.java** should match the Bison token definitions:

```java
public enum TokenType {
    // Keywords
    VAR, TYPE, IS, INTEGER, REAL, BOOLEAN, ARRAY, RECORD, END,
    WHILE, LOOP, FOR, IN, REVERSE, IF, THEN, ELSE, PRINT, ROUTINE,
    TRUE, FALSE, AND, OR, XOR, NOT,

    // Operators
    ASSIGN, DOTDOT, PLUS, MINUS, MUL, DIV, MOD,
    LT, LE, GT, GE, EQ, NE,

    // Delimiters
    COLON, COMMA, DOT, LPAREN, RPAREN, LBRACKET, RBRACKET, ARROW,

    // Literals
    IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL,

    // Special
    EOF
}
```

**Token.java**:
```java
public class Token {
    private TokenType type;
    private String lexeme;
    private int line;

    // Constructor, getters
}
```

**Lexer.java**:
```java
public class Lexer {
    private String input;
    private int position;
    private int line;

    public Token nextToken() {
        // Tokenization logic
    }

    // JNI methods
    public native void initializeParser();
    public native boolean parseInput(String input);
}
```

### 2. C++ Side (JNI Implementation)

**lexer.h** (already created as stub):

```cpp
class JavaLexer {
private:
    JNIEnv* env;
    jobject lexerInstance;
    // JNI method IDs

public:
    JavaLexer();
    ~JavaLexer();
    int nextToken();
    const char* getLexeme();
    int getType();
    int getLine();
};
```

**JNI Implementation** (separate file, e.g., `jni_lexer.cpp`):

```cpp
#include <jni.h>
#include "lexer.h"

JNIEXPORT void JNICALL Java_compiler_lexer_Lexer_initializeParser
  (JNIEnv *env, jobject obj) {
    // Initialize parser
}

JNIEXPORT jboolean JNICALL Java_compiler_lexer_Lexer_parseInput
  (JNIEnv *env, jobject obj, jstring input) {
    // Parse input and return success
}
```

### 3. Build Configuration

**CMakeLists.txt** or **Makefile** additions:

```cmake
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

add_library(parser SHARED
    parser.tab.c
    lex.yy.c
    ast.cpp
    symbol.cpp
    lexer.cpp
    jni_lexer.cpp
)

target_link_libraries(parser ${JNI_LIBRARIES})
```

## WASM Code Generation Interface

### 1. WASMGenerator Class

The `WASMGenerator` class (stub in parser.y) should implement:

```cpp
class WASMGenerator {
private:
    std::ostream& output;

public:
    WASMGenerator(std::ostream& out = std::cout) : output(out) {}

    void generate(ProgramNode* root) {
        // Traverse AST and generate WASM
        output << "(module\n";
        generateProgram(root);
        output << ")\n";
    }

private:
    void generateProgram(ProgramNode* node);
    void generateDeclaration(DeclarationNode* node);
    void generateStatement(StatementNode* node);
    void generateExpression(ExpressionNode* node);
    // ... other generation methods
};
```

### 2. AST Traversal

Implement visitor pattern or recursive traversal:

```cpp
void WASMGenerator::generateExpression(ExpressionNode* node) {
    if (auto* intLit = dynamic_cast<IntegerLiteralNode*>(node)) {
        output << "(i32.const " << intLit->value << ")\n";
    } else if (auto* binOp = dynamic_cast<BinaryOpNode*>(node)) {
        generateExpression(binOp->left);
        generateExpression(binOp->right);
        switch (binOp->op) {
            case OpKind::PLUS:
                output << "(i32.add)\n";
                break;
            // ... other operators
        }
    }
    // ... other node types
}
```

### 3. Memory Management

For reference types (arrays, records):

```cpp
void WASMGenerator::generateArrayType(ArrayTypeNode* node) {
    // Generate WASM memory allocation
    output << "(memory.grow (i32.const " << calculateSize(node) << "))\n";
}
```

### 4. Function Generation

For routines:

```cpp
void WASMGenerator::generateRoutine(RoutineDeclarationNode* node) {
    output << "(func $" << node->header->name;
    // Parameters
    for (auto* param : node->header->parameters->parameters) {
        output << " (param $" << param->name << " i32)";
    }
    // Return type
    if (node->header->returnType) {
        output << " (result i32)";
    }
    output << "\n";

    // Body
    generateBody(node->body);
    output << ")\n";
}
```

## Complete Integration Example

```cpp
#include "parser.h"
#include "wasm_generator.h"

int main(int argc, char** argv) {
    // Initialize JVM
    JavaVM* jvm;
    JNIEnv* env;
    // ... JVM initialization

    // Create lexer instance
    jclass lexerClass = env->FindClass("compiler/lexer/Lexer");
    jobject lexer = env->NewObject(lexerClass, /* constructor */);

    // Parse input
    symbolTable = new SymbolTable();
    javaLexer = new JavaLexer(env, lexer);

    yyparse(); // Populates astRoot

    // Generate WASM
    WASMGenerator generator;
    generator.generate(astRoot);

    // Cleanup
    delete symbolTable;
    delete javaLexer;

    return 0;
}
```

## Testing Integration

1. Compile Java code with JNI headers
2. Compile C++ code with JNI
3. Run parser with test inputs
4. Verify WASM output with WebAssembly tools

## Performance Considerations

- Use efficient AST traversal (avoid deep recursion)
- Implement symbol table with hash maps
- Cache type information to avoid repeated lookups
- Use streaming output for large WASM files
