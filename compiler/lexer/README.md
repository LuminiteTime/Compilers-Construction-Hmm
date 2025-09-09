# Imperative (I) Language Lexer Implementation

## Overview

This is a complete, error-free lexer implementation for the Imperative (I) language, developed according to the specification in `docs/lexer_scope.md`. The lexer integrates with Bison-generated parsers and targets WebAssembly (WASM) as the eventual platform, though the lexer itself is platform-agnostic.

## Architecture

The lexer is implemented using a finite state machine (FSM) approach with manual character-by-character scanning. It follows the maximal munch principle and provides accurate position tracking with line and column information.

### Key Components

1. **TokenType.java** - Enum defining all terminal symbols
2. **Token.java** - Token representation with lexeme and position data
3. **LexerException.java** - Exception handling for lexical errors
4. **Lexer.java** - Main lexer implementation with FSM scanning
5. **TestLexer.java** - Comprehensive test suite

## Features

- **Unicode Support**: Processes Unicode characters from single disk files
- **Accurate Position Tracking**: Line and column numbers (1-based indexing)
- **Error Handling**: Detailed LexerException with position information
- **Comment Support**: Single-line (//) and multi-line (/* */) comments
- **String Literals**: Support for escape sequences in strings
- **Keyword Recognition**: O(1) lookup using HashMap
- **Maximal Munch**: Longest possible token recognition
- **Whitespace Handling**: Automatic skipping of spaces, tabs, and newlines

## Token Categories

### Keywords (reserved words)
- **Declarations**: `var`, `type`, `is`, `integer`, `real`, `boolean`, `array`, `record`, `end`
- **Statements**: `while`, `loop`, `for`, `in`, `reverse`, `if`, `then`, `else`, `print`, `routine`
- **Expressions/Booleans**: `true`, `false`, `and`, `or`, `xor`, `not`
- **Other**: `return`

### Literals
- **Integer Literals**: Decimal digits with optional sign (`+`, `-`)
- **Real Literals**: Decimal format with optional sign
- **String Literals**: Double-quoted strings with escape sequences
- **Boolean Literals**: `true`, `false` (treated as keywords)

### Operators
- **Arithmetic**: `+`, `-`, `*`, `/`, `%`
- **Relational**: `<`, `<=`, `>`, `>=`, `=`, `/=`
- **Logical**: `and`, `or`, `xor`, `not`
- **Assignment**: `:=`
- **Range**: `..`

### Delimiters/Punctuation
- `:`, `;`, `,`, `.`, `(`, `)`, `[`, `]`

## Test Results

Below are the first 10 tokens for each test case from the specification:

### Test 1: Variable Declarations
```
VAR:var@1:1
IDENTIFIER:x@1:5
COLON::@1:6
INTEGER:integer@1:8
IS:is@1:16
INTEGER_LITERAL:42@1:19
SEMICOLON:;@1:21
VAR:var@2:1
IDENTIFIER:y@2:5
```

### Test 2: Arrays & Data Structures
```
VAR:var@1:1
IDENTIFIER:numbers@1:5
COLON::@1:12
ARRAY:array@1:14
LBRACKET:[@1:19
INTEGER_LITERAL:5@1:20
RBRACKET:]@1:21
INTEGER:integer@1:23
SEMICOLON:;@1:30
IDENTIFIER:numbers@2:1
```

### Test 3: Record Types
```
TYPE:type@1:1
IDENTIFIER:Point@1:6
IS:is@1:12
RECORD:record@1:15
VAR:var@2:5
IDENTIFIER:x@2:9
COLON::@2:10
REAL:real@2:12
SEMICOLON:;@2:16
VAR:var@3:5
```

### Test 4: While Loops
```
VAR:var@1:1
IDENTIFIER:counter@1:5
COLON::@1:12
INTEGER:integer@1:14
IS:is@1:22
INTEGER_LITERAL:10@1:25
SEMICOLON:;@1:27
WHILE:while@2:1
IDENTIFIER:counter@2:7
GREATER:>@2:15
INTEGER_LITERAL:0@2:17
```

### Test 5: For Loops
```
FOR:for@1:1
IDENTIFIER:i@1:5
IN:in@1:7
INTEGER_LITERAL:1@1:10
RANGE:..@1:11
INTEGER_LITERAL:10@1:13
LOOP:loop@1:16
PRINT:print@2:5
IDENTIFIER:i@2:11
MULTIPLY:*@2:13
IDENTIFIER:i@2:15
SEMICOLON:;@2:16
```

### Test 6: Functions & Recursion
```
ROUTINE:routine@1:1
IDENTIFIER:factorial@1:9
LPAREN:(@1:18
IDENTIFIER:n@1:19
COLON::@1:20
INTEGER:integer@1:22
RPAREN: )@1:29
COLON::@1:30
INTEGER:integer@1:32
IS:is@1:40
IF:if@2:5
IDENTIFIER:n@2:8
```

### Test 7: Type Conversions
```
VAR:var@1:1
IDENTIFIER:i@1:5
COLON::@1:6
INTEGER:integer@1:8
IS:is@1:16
INTEGER_LITERAL:42@1:19
SEMICOLON:;@1:21
VAR:var@2:1
IDENTIFIER:r@2:5
COLON::@2:6
REAL:real@2:8
IS:is@2:12
IDENTIFIER:i@2:15
```

### Test 8: Error Detection
```
VAR:var@1:1
IDENTIFIER:flag@1:5
COLON::@1:9
BOOLEAN:boolean@1:11
IS:is@1:19
REAL_LITERAL:3.14@1:22
SEMICOLON:;@1:26
EOF:@2:1
```

### Test 9: Operator Precedence
```
VAR:var@1:1
IDENTIFIER:result@1:5
COLON::@1:11
INTEGER:integer@1:13
IS:is@1:21
INTEGER_LITERAL:2@1:24
PLUS:+@1:26
INTEGER_LITERAL:3@1:28
MULTIPLY:*@1:30
INTEGER_LITERAL:4@1:32
MINUS:-@1:34
INTEGER_LITERAL:1@1:36
SEMICOLON:;@1:37
```

### Test 10: Complex Data Structures
```
TYPE:type@1:1
IDENTIFIER:Student@1:6
IS:is@1:14
RECORD:record@1:17
VAR:var@2:5
IDENTIFIER:id@2:9
COLON::@2:11
INTEGER:integer@2:13
SEMICOLON:;@2:20
VAR:var@3:5
IDENTIFIER:grade@3:9
COLON::@3:15
REAL:real@3:17
```

### Test 11: Nested Records
```
TYPE:type@1:1
IDENTIFIER:Address@1:6
IS:is@1:14
RECORD:record@1:17
VAR:var@2:5
IDENTIFIER:street@2:9
COLON::@2:15
IDENTIFIER:string@2:17
SEMICOLON:;@2:23
VAR:var@3:5
IDENTIFIER:city@3:9
COLON::@3:13
IDENTIFIER:string@3:15
SEMICOLON:;@3:21
VAR:var@4:5
IDENTIFIER:zip@4:9
COLON::@4:12
INTEGER:integer@4:14
SEMICOLON:;@4:21
END:end@5:1
TYPE:type@7:1
IDENTIFIER:Person@7:6
IS:is@7:13
RECORD:record@7:16
VAR:var@8:5
IDENTIFIER:name@8:9
COLON::@8:13
IDENTIFIER:string@8:15
SEMICOLON:;@8:21
VAR:var@9:5
IDENTIFIER:age@9:9
COLON::@9:12
INTEGER:integer@9:14
SEMICOLON:;@9:21
VAR:var@10:5
IDENTIFIER:address@10:9
COLON::@10:16
IDENTIFIER:Address@10:18
SEMICOLON:;@10:25
END:end@11:1
VAR:var@13:1
IDENTIFIER:person@13:5
COLON::@13:11
IDENTIFIER:Person@13:13
SEMICOLON:;@13:19
IDENTIFIER:person@14:1
DOT:.@14:7
IDENTIFIER:name@14:8
ASSIGN::=@14:13
STRING_LITERAL:"John Doe"@14:16
SEMICOLON:;@14:26
IDENTIFIER:person@15:1
DOT:.@15:7
IDENTIFIER:age@15:8
ASSIGN::=@15:12
INTEGER_LITERAL:30@15:15
SEMICOLON:;@15:17
IDENTIFIER:person@16:1
DOT:.@16:7
IDENTIFIER:address@16:8
DOT:.@16:15
IDENTIFIER:street@16:16
ASSIGN::=@16:23
STRING_LITERAL:"123 Main St"@16:26
SEMICOLON:;@16:39
IDENTIFIER:person@17:1
DOT:.@17:7
IDENTIFIER:address@17:8
DOT:.@17:15
IDENTIFIER:city@17:16
ASSIGN::=@17:21
STRING_LITERAL:"New York"@17:24
SEMICOLON:;@17:34
IDENTIFIER:person@18:1
DOT:.@18:7
IDENTIFIER:address@18:8
DOT:.@18:15
IDENTIFIER:zip@18:16
ASSIGN::=@18:20
INTEGER_LITERAL:10001@18:23
SEMICOLON:;@18:28
EOF:@19:1
```

## Usage

### Compilation
```bash
javac compiler/lexer/*.java
```

### Running Tests
```bash
java -cp compiler/lexer TestLexer
```

### Integration with Bison
The TokenType enum values can be mapped to Bison %token declarations:
```bison
%token <type> VAR TYPE IS INTEGER REAL BOOLEAN ARRAY RECORD END
%token <type> WHILE LOOP FOR IN REVERSE IF THEN ELSE PRINT ROUTINE
%token <type> TRUE FALSE AND OR XOR NOT RETURN
%token <type> IDENTIFIER INTEGER_LITERAL REAL_LITERAL STRING_LITERAL
%token <type> PLUS MINUS MULTIPLY DIVIDE MODULO
%token <type> LESS LESS_EQUAL GREATER GREATER_EQUAL EQUAL NOT_EQUAL
%token <type> ASSIGN RANGE
%token <type> COLON SEMICOLON COMMA DOT LPAREN RPAREN LBRACKET RBRACKET
%token <type> EOF
```

## Implementation Notes

- **Finite State Machine**: Manual implementation with character lookahead
- **Memory Efficiency**: No external dependencies, pure Java standard library
- **Thread Safety**: Single-threaded design as required for lexical analysis
- **Error Recovery**: Comprehensive error reporting with exact position information
- **Unicode Handling**: Full Unicode support through Java's Reader interface

All tests pass without LexerException (except Test 8, which is semantically invalid but lexically correct). Test 11 demonstrates proper handling of nested record structures with multi-level dot notation access.
