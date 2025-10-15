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

// Utility function declarations
TypeNode* inferType(ExpressionNode* expr);
bool checkAssignmentTypes(ExpressionNode* target, ExpressionNode* value);
bool isBooleanType(ExpressionNode* expr);
bool checkArguments(RoutineInfo* routine, ASTNode* arguments);

// WASM Generator stub
class WASMGenerator {
public:
    void generate(ProgramNode* root) {
        // Stub implementation
        std::cout << "Generating WASM from AST" << std::endl;
    }
};
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