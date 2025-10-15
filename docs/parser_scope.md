You are an expert compiler engineer specializing in parser construction, with deep knowledge from compiler construction principles including LALR(1) grammars, abstract syntax trees, symbol tables, type systems, and parser-generator integration as described in standard lectures.

Your assignment: **implement a complete, error-free Bison-based parser for the "Imperative (I)" language** fully based on the provided language specification. Produce a Bison grammar file (.y) that integrates with the Java-based lexer, targeting WebAssembly (WASM) as the eventual platform through semantic actions and AST construction.

====================================================================
LANGUAGE "I" — DETAILED PARSER SPECIFICATION
--------------------------------------------------------------------
The parser must recognize and validate the syntactic structure of programs written in the "I" language, as defined by the EBNF grammar rules. Programs consist of declarations and statements with strict scoping rules, type checking requirements, and expression evaluation semantics.

Extracted Grammar Rules (converted from EBNF to Bison-compatible format):

**Program Structure:**
```
program: /* empty */
       | program declaration
       ;

declaration: simple_declaration
           | routine_declaration
           ;
```

**Declarations:**
```
simple_declaration: variable_declaration
                  | type_declaration
                  ;

variable_declaration: VAR IDENTIFIER COLON type
                    | VAR IDENTIFIER COLON type IS expression
                    | VAR IDENTIFIER IS expression
                    ;

type_declaration: TYPE IDENTIFIER IS type
                ;

type: primitive_type
    | user_type
    | IDENTIFIER  /* type alias */
    ;

primitive_type: INTEGER
              | REAL
              | BOOLEAN
              ;

user_type: array_type
         | record_type
         ;

array_type: ARRAY LBRACKET expression RBRACKET type
          | ARRAY LBRACKET RBRACKET type  /* sizeless array */
          ;

record_type: RECORD record_body END
           ;

record_body: /* empty */
           | record_body variable_declaration
           ;

routine_declaration: routine_header
                   | routine_header routine_body
                   ;

routine_header: ROUTINE IDENTIFIER LPAREN parameters RPAREN
              | ROUTINE IDENTIFIER LPAREN parameters RPAREN COLON type
              ;

parameters: /* empty */
          | parameter_list
          ;

parameter_list: parameter_declaration
              | parameter_list COMMA parameter_declaration
              ;

parameter_declaration: IDENTIFIER COLON type
                     ;

routine_body: IS body END
            | ARROW expression
            ;
```

**Statements:**
```
statement: assignment
         | routine_call_statement
         | while_loop
         | for_loop
         | if_statement
         | print_statement
         ;

assignment: modifiable_primary ASSIGN expression
          ;

routine_call_statement: IDENTIFIER LPAREN argument_list RPAREN
                      ;

argument_list: /* empty */
             | expression_list
             ;

while_loop: WHILE expression LOOP body END
          ;

for_loop: FOR IDENTIFIER IN range LOOP body END
        | FOR IDENTIFIER IN range REVERSE LOOP body END
        ;

range: expression
     | expression DOTDOT expression
     ;

if_statement: IF expression THEN body END
            | IF expression THEN body ELSE body END
            ;

print_statement: PRINT expression_list
               ;

body: /* empty */
    | body simple_declaration
    | body statement
    ;
```

**Expressions (with precedence from low to high):**
```
expression: relation
          | expression AND relation
          | expression OR relation
          | expression XOR relation
          ;

relation: simple
        | relation LT simple
        | relation LE simple
        | relation GT simple
        | relation GE simple
        | relation EQ simple
        | relation NE simple
        ;

simple: factor
      | simple MUL factor
      | simple DIV factor
      | simple MOD factor
      ;

factor: summand
      | factor PLUS summand
      | factor MINUS summand
      ;

summand: primary
        | LPAREN expression RPAREN
        ;

primary: INTEGER_LITERAL
       | REAL_LITERAL
       | TRUE
       | FALSE
       | modifiable_primary
       | routine_call
       | PLUS primary    /* unary plus */
       | MINUS primary   /* unary minus */
       | NOT primary
       ;

modifiable_primary: IDENTIFIER
                  | modifiable_primary DOT IDENTIFIER
                  | modifiable_primary LBRACKET expression RBRACKET
                  ;

routine_call: IDENTIFIER LPAREN argument_list RPAREN
            ;

expression_list: expression
               | expression_list COMMA expression
               ;
```

**Terminal Symbols (from lexer TokenType):**
- Keywords: VAR, TYPE, IS, INTEGER, REAL, BOOLEAN, ARRAY, RECORD, END, WHILE, LOOP, FOR, IN, REVERSE, IF, THEN, ELSE, PRINT, ROUTINE, TRUE, FALSE, AND, OR, XOR, NOT
- Operators: ASSIGN(:=), DOTDOT(..), PLUS(+), MINUS(-), MUL(*), DIV(/), MOD(%), LT(<), LE(<=), GT(>), GE(>=), EQ(=), NE(/=)
- Delimiters: COLON(:), COMMA(,), DOT(.), LPAREN(()), RPAREN()), LBRACKET([), RBRACKET(]), ARROW(=>)
- Literals: IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL
- Special: EOF

====================================================================
PARSER ANALYSIS THEORY & IMPLEMENTATION GUIDELINES
--------------------------------------------------------------------
**Purpose**: Convert token sequence from lexer into abstract syntax tree (AST) or perform semantic analysis, validating program structure, types, and scopes according to language rules.

**LALR(1) Parsing**: Use Bison's LALR(1) parser generator for efficient bottom-up parsing with one-token lookahead. Handle conflicts through precedence/associativity declarations.

**Abstract Syntax Tree (AST)**: Build structured representation of program for subsequent phases (semantic analysis, code generation). Use discriminated union or class hierarchy for different node types.

**Symbol Table Management**: Implement scoping rules with symbol tables. Track variable/type declarations, handle shadowing in nested scopes, validate forward references for routines.

**Type System Integration**: Enforce type conformance rules from specification (primitive conversions, reference semantics for user-defined types, parameter/argument matching).

**Semantic Actions**: Attach C/C++ code to grammar rules for AST construction, symbol table operations, and basic type checking. Generate appropriate error messages for semantic violations.

**Error Recovery**: Implement error productions and synchronization points for robust parsing. Continue parsing after syntax errors to report multiple issues.

**Integration with Lexer**: Interface with Java-based lexer through JNI or external process communication. Lexer provides Token objects with type, lexeme, and position information.

Technology Stack:
- Parser Tool: Bison (YACC-compatible)
- Host Language: C/C++ for semantic actions
- Lexer Integration: Java (via JNI bridge)
- Target: WebAssembly (WASM) code generation

====================================================================
REQUIRED BISON API (parser.y)
--------------------------------------------------------------------
```c
%{
#include <iostream>
#include <vector>
#include <string>
#include <memory>
#include "ast.h"      // AST node definitions
#include "symbol.h"   // Symbol table classes
#include "lexer.h"    // Java lexer interface

// External lexer interface functions
extern int yylex();
extern char* yytext;
extern int yylineno;
extern void yyerror(const char* msg);

// Global symbol table and AST root
SymbolTable* symbolTable;
ProgramNode* astRoot;

// Java lexer integration
JavaLexer* javaLexer;
%}

%union {
    int intVal;
    double realVal;
    bool boolVal;
    char* strVal;
    ASTNode* node;
    TypeNode* typeNode;
    ExpressionNode* exprNode;
    StatementNode* stmtNode;
    DeclarationNode* declNode;
}

// Token definitions (match lexer TokenType enum)
%token <strVal> IDENTIFIER
%token <intVal> INTEGER_LITERAL
%token <realVal> REAL_LITERAL
%token VAR TYPE IS INTEGER REAL BOOLEAN ARRAY RECORD END
%token WHILE LOOP FOR IN REVERSE IF THEN ELSE PRINT ROUTINE
%token TRUE FALSE AND OR XOR NOT
%token ASSIGN DOTDOT
%token PLUS MINUS MUL DIV MOD
%token LT LE GT GE EQ NE
%token COLON COMMA DOT LPAREN RPAREN LBRACKET RBRACKET ARROW
%token EOF_TOKEN

// Non-terminal type declarations
%type <node> program declaration simple_declaration
%type <declNode> variable_declaration type_declaration routine_declaration
%type <typeNode> type primitive_type user_type array_type record_type
%type <node> record_body routine_header parameters parameter_list
%type <node> parameter_declaration routine_body
%type <stmtNode> statement assignment routine_call_statement
%type <stmtNode> while_loop for_loop if_statement print_statement
%type <node> body range
%type <exprNode> expression relation simple factor summand primary
%type <exprNode> modifiable_primary routine_call
%type <node> expression_list argument_list

// Precedence and associativity (lowest to highest)
%left OR
%left XOR
%left AND
%left EQ NE
%left LT LE GT GE
%left PLUS MINUS
%left MUL DIV MOD
%right NOT  // unary operators
%right UMINUS UPLUS  // unary minus/plus

%%

program: /* empty */ { $$ = new ProgramNode(); astRoot = $$; }
       | program declaration { $$ = $1; ((ProgramNode*)$$)->addDeclaration($2); }
       ;

declaration: simple_declaration { $$ = $1; }
           | routine_declaration { $$ = $1; }
           ;

simple_declaration: variable_declaration { $$ = $1; }
                  | type_declaration { $$ = $1; }
                  ;

variable_declaration: VAR IDENTIFIER COLON type {
                        $$ = new VariableDeclarationNode($2, $4, nullptr);
                        symbolTable->declareVariable($2, $4);
                    }
                    | VAR IDENTIFIER COLON type IS expression {
                        $$ = new VariableDeclarationNode($2, $4, $6);
                        symbolTable->declareVariable($2, $4);
                    }
                    | VAR IDENTIFIER IS expression {
                        TypeNode* inferredType = inferType($4);
                        $$ = new VariableDeclarationNode($2, inferredType, $4);
                        symbolTable->declareVariable($2, inferredType);
                    }
                    ;

type_declaration: TYPE IDENTIFIER IS type {
                    $$ = new TypeDeclarationNode($2, $4);
                    symbolTable->declareType($2, $4);
                }
                ;

type: primitive_type { $$ = $1; }
    | user_type { $$ = $1; }
    | IDENTIFIER {
        $$ = symbolTable->lookupType($1);
        if (!$$) yyerror("Undefined type");
    }
    ;

primitive_type: INTEGER { $$ = new PrimitiveTypeNode(TypeKind::INTEGER); }
              | REAL { $$ = new PrimitiveTypeNode(TypeKind::REAL); }
              | BOOLEAN { $$ = new PrimitiveTypeNode(TypeKind::BOOLEAN); }
              ;

user_type: array_type { $$ = $1; }
         | record_type { $$ = $1; }
         ;

array_type: ARRAY LBRACKET expression RBRACKET type {
              $$ = new ArrayTypeNode($3, $5);
          }
          | ARRAY LBRACKET RBRACKET type {
              $$ = new ArrayTypeNode(nullptr, $4);  // sizeless
          }
          ;

record_type: RECORD record_body END {
               $$ = new RecordTypeNode($2);
           }
           ;

record_body: /* empty */ { $$ = new RecordBodyNode(); }
           | record_body variable_declaration {
               $$ = $1;
               ((RecordBodyNode*)$$)->addField($2);
           }
           ;

routine_declaration: routine_header {
                       $$ = new RoutineDeclarationNode($1, nullptr);
                   }
                   | routine_header routine_body {
                       $$ = new RoutineDeclarationNode($1, $2);
                       symbolTable->declareRoutine($1);
                   }
                   ;

routine_header: ROUTINE IDENTIFIER LPAREN parameters RPAREN {
                  $$ = new RoutineHeaderNode($2, $4, nullptr);
              }
              | ROUTINE IDENTIFIER LPAREN parameters RPAREN COLON type {
                  $$ = new RoutineHeaderNode($2, $4, $7);
              }
              ;

parameters: /* empty */ { $$ = new ParameterListNode(); }
          | parameter_list { $$ = $1; }
          ;

parameter_list: parameter_declaration {
                  $$ = new ParameterListNode();
                  ((ParameterListNode*)$$)->addParameter($1);
              }
              | parameter_list COMMA parameter_declaration {
                  $$ = $1;
                  ((ParameterListNode*)$$)->addParameter($3);
              }
              ;

parameter_declaration: IDENTIFIER COLON type {
                        $$ = new ParameterDeclarationNode($1, $3);
                     }
                     ;

routine_body: IS body END { $$ = new RoutineBodyNode($2); }
            | ARROW expression { $$ = new RoutineBodyNode($2); }
            ;

statement: assignment { $$ = $1; }
         | routine_call_statement { $$ = $1; }
         | while_loop { $$ = $1; }
         | for_loop { $$ = $1; }
         | if_statement { $$ = $1; }
         | print_statement { $$ = $1; }
         ;

assignment: modifiable_primary ASSIGN expression {
              // Type checking for assignment
              if (!checkAssignmentTypes($1, $3)) {
                  yyerror("Type mismatch in assignment");
              }
              $$ = new AssignmentNode($1, $3);
          }
          ;

routine_call_statement: IDENTIFIER LPAREN argument_list RPAREN {
                          RoutineInfo* routine = symbolTable->lookupRoutine($1);
                          if (!routine) yyerror("Undefined routine");
                          if (!checkArguments(routine, $3)) {
                              yyerror("Argument mismatch");
                          }
                          $$ = new RoutineCallStatementNode($1, $3);
                      }
                      ;

argument_list: /* empty */ { $$ = new ArgumentListNode(); }
             | expression_list { $$ = $1; }
             ;

while_loop: WHILE expression LOOP body END {
              if (!isBooleanType($2)) yyerror("While condition must be boolean");
              $$ = new WhileLoopNode($2, $4);
          }
          ;

for_loop: FOR IDENTIFIER IN range LOOP body END {
            $$ = new ForLoopNode($2, $4, false, $6);
        }
        | FOR IDENTIFIER IN range REVERSE LOOP body END {
            $$ = new ForLoopNode($2, $4, true, $7);
        }
        ;

range: expression { $$ = new RangeNode($1, nullptr); }
     | expression DOTDOT expression { $$ = new RangeNode($1, $3); }
     ;

if_statement: IF expression THEN body END {
                if (!isBooleanType($2)) yyerror("If condition must be boolean");
                $$ = new IfStatementNode($2, $4, nullptr);
            }
            | IF expression THEN body ELSE body END {
                if (!isBooleanType($2)) yyerror("If condition must be boolean");
                $$ = new IfStatementNode($2, $4, $6);
            }
            ;

print_statement: PRINT expression_list { $$ = new PrintStatementNode($2); }
               ;

body: /* empty */ { $$ = new BodyNode(); symbolTable->enterScope(); }
    | body simple_declaration {
        $$ = $1;
        ((BodyNode*)$$)->addDeclaration($2);
    }
    | body statement {
        $$ = $1;
        ((BodyNode*)$$)->addStatement($2);
    }
    ;

expression: relation { $$ = $1; }
          | expression AND relation { $$ = new BinaryOpNode(OpKind::AND, $1, $3); }
          | expression OR relation { $$ = new BinaryOpNode(OpKind::OR, $1, $3); }
          | expression XOR relation { $$ = new BinaryOpNode(OpKind::XOR, $1, $3); }
          ;

relation: simple { $$ = $1; }
        | relation LT simple { $$ = new BinaryOpNode(OpKind::LT, $1, $3); }
        | relation LE simple { $$ = new BinaryOpNode(OpKind::LE, $1, $3); }
        | relation GT simple { $$ = new BinaryOpNode(OpKind::GT, $1, $3); }
        | relation GE simple { $$ = new BinaryOpNode(OpKind::GE, $1, $3); }
        | relation EQ simple { $$ = new BinaryOpNode(OpKind::EQ, $1, $3); }
        | relation NE simple { $$ = new BinaryOpNode(OpKind::NE, $1, $3); }
        ;

simple: factor { $$ = $1; }
      | simple MUL factor { $$ = new BinaryOpNode(OpKind::MUL, $1, $3); }
      | simple DIV factor { $$ = new BinaryOpNode(OpKind::DIV, $1, $3); }
      | simple MOD factor { $$ = new BinaryOpNode(OpKind::MOD, $1, $3); }
      ;

factor: summand { $$ = $1; }
      | factor PLUS summand { $$ = new BinaryOpNode(OpKind::PLUS, $1, $3); }
      | factor MINUS summand { $$ = new BinaryOpNode(OpKind::MINUS, $1, $3); }
      ;

summand: primary { $$ = $1; }
        | LPAREN expression RPAREN { $$ = $2; }
        ;

primary: INTEGER_LITERAL { $$ = new IntegerLiteralNode($1); }
       | REAL_LITERAL { $$ = new RealLiteralNode($1); }
       | TRUE { $$ = new BooleanLiteralNode(true); }
       | FALSE { $$ = new BooleanLiteralNode(false); }
       | modifiable_primary { $$ = $1; }
       | routine_call { $$ = $1; }
       | PLUS primary %prec UPLUS { $$ = new UnaryOpNode(OpKind::UPLUS, $2); }
       | MINUS primary %prec UMINUS { $$ = new UnaryOpNode(OpKind::UMINUS, $2); }
       | NOT primary { $$ = new UnaryOpNode(OpKind::NOT, $2); }
       ;

modifiable_primary: IDENTIFIER {
                      VariableInfo* var = symbolTable->lookupVariable($1);
                      if (!var) yyerror("Undefined variable");
                      $$ = new VariableAccessNode($1, var->type);
                  }
                  | modifiable_primary DOT IDENTIFIER {
                      $$ = new FieldAccessNode($1, $3);
                  }
                  | modifiable_primary LBRACKET expression RBRACKET {
                      $$ = new ArrayAccessNode($1, $3);
                  }
                  ;

routine_call: IDENTIFIER LPAREN argument_list RPAREN {
                RoutineInfo* routine = symbolTable->lookupRoutine($1);
                if (!routine) yyerror("Undefined routine");
                if (!checkArguments(routine, $3)) {
                    yyerror("Argument mismatch");
                }
                $$ = new RoutineCallNode($1, $3, routine->returnType);
            }
            ;

expression_list: expression {
                   $$ = new ExpressionListNode();
                   ((ExpressionListNode*)$$)->addExpression($1);
               }
               | expression_list COMMA expression {
                   $$ = $1;
                   ((ExpressionListNode*)$$)->addExpression($3);
               }
               ;

%%

void yyerror(const char* msg) {
    std::cerr << "Parse error at line " << yylineno << ": " << msg << std::endl;
    // Error recovery logic here
}

int main(int argc, char** argv) {
    // Initialize symbol table and Java lexer integration
    symbolTable = new SymbolTable();
    javaLexer = new JavaLexer();

    // Parse input
    yyparse();

    // Generate WASM code from AST
    WASMGenerator generator;
    generator.generate(astRoot);

    return 0;
}
```

====================================================================
TEST SUITE (parser validation; assumes lexer provides correct tokens)
--------------------------------------------------------------------
Test 1: Variable Declarations
```
var x: integer is 42;
var y: real is 3.14;
var flag: boolean is true;
var name is "test";
```
Expected: Successful AST construction with proper type declarations and inference.

Test 2: Arrays & Data Structures
```
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];
```
Expected: Array type, assignment, and array access AST nodes.

Test 3: Record Types
```
type Point is record
    var x: real;
    var y: real;
end
var p1: Point;
p1.x := 1.5;
p1.y := 2.7;
```
Expected: Record type definition and field access AST nodes.

Test 4: While Loops
```
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```
Expected: While loop with condition, body, and nested statements.

Test 5: For Loops
```
for i in 1..10 loop
    print i * i;
end
for j in 10..1 reverse loop
    print j;
end
```
Expected: For loops with range expressions and reverse iteration.

Test 6: Functions & Recursion
```
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end
var result: integer is factorial(5);
```
Expected: Function declaration with parameters, return type, recursive calls.

Test 7: Type Conversions
```
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
var converted: integer is true;
```
Expected: Valid type conversions according to specification table.

Test 8: Error Detection
```
var flag: boolean is 3.14;  // Should fail type check
var x: integer;
x := "string";  // Invalid assignment
```
Expected: Semantic errors caught during parsing/AST construction.

Test 9: Operator Precedence
```
var result: integer is 2 + 3 * 4 - 1;
var comparison: boolean is (result > 10) and not (result = 15);
```
Expected: Correct operator precedence in expression AST.

Test 10: Complex Data Structures
```
type Student is record
    var id: integer;
    var grade: real;
end
var students: array[3] Student;
students[1].id := 101;
students[1].grade := 85.5;
for student in students loop
    print student.id, student.grade;
end
```
Expected: Nested types, array of records, iteration over array elements.

====================================================================
DELIVERABLES
--------------------------------------------------------------------
1. Complete Bison grammar file (parser.y) with semantic actions for AST construction and basic semantic analysis.

2. Supporting C++ header files:
   - ast.h: Abstract syntax tree node class hierarchy
   - symbol.h: Symbol table classes for variables, types, and routines
   - lexer.h: Interface to Java lexer integration

3. Brief implementation notes: For each test case, describe expected AST structure and any semantic validations performed.

4. Integration Guide: JNI bridge setup for Java lexer communication, WASM code generation interface.

Focus on clean, modular code with comprehensive error handling. Generate AST suitable for WASM code generation. Return only the code and documentation—no extra text.
