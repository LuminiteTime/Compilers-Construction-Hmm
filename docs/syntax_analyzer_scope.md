You are an expert compiler engineer specializing in syntax analysis, with deep knowledge from compiler construction principles including context-free grammars, parsing techniques, abstract syntax trees, and lexer-parser integration as described in standard compiler theory.

Your assignment: **implement a complete, error-free recursive descent parser for the "Imperative (I)" language** fully based on the provided grammar specification. Produce Java code that integrates with the existing lexer, targeting WebAssembly (WASM) as the eventual platform, though the parser itself is platform-agnostic.

====================================================================
LANGUAGE "I" — DETAILED GRAMMAR SPECIFICATION
--------------------------------------------------------------------
The language processes tokens from the lexer as a sequence of terminal symbols. Syntactically, a program is a sequence of declarations (variables, types, subprograms) separated by newlines or semicolons. All entities must be declared before use, with scopes spanning from declaration to the end of the block or program.

Extracted Grammar Rules (from EBNF specification):

1. Program Structure
   - Program: { SimpleDeclaration | RoutineDeclaration }
   - SimpleDeclaration: VariableDeclaration | TypeDeclaration
   - RoutineDeclaration: RoutineHeader [ RoutineBody ]

2. Variable Declarations
   - VariableDeclaration: var Identifier : Type [ is Expression ] | var Identifier is Expression
   - Type: PrimitiveType | UserType | Identifier
   - PrimitiveType: integer | real | boolean
   - UserType: ArrayType | RecordType
   - ArrayType: array [ [ Expression ] ] Type
   - RecordType: record { VariableDeclaration } end

3. Type Declarations
   - TypeDeclaration: type Identifier is Type

4. Routine Declarations
   - RoutineHeader: routine Identifier ( Parameters ) [ : Type ]
   - Parameters: ParameterDeclaration { , ParameterDeclaration } | ε
   - ParameterDeclaration: Identifier : Type
   - RoutineBody: is Body end | => Expression

5. Statements
   - Statement: Assignment | RoutineCall | WhileLoop | ForLoop | IfStatement | PrintStatement
   - Assignment: ModifiablePrimary := Expression
   - RoutineCall: Identifier [ ( Expression { , Expression } ) ]
   - WhileLoop: while Expression loop Body end
   - ForLoop: for Identifier in Range [ reverse ] loop Body end
   - Range: Expression [ .. Expression ]
   - IfStatement: if Expression then Body [ else Body ] end
   - PrintStatement: print Expression { , Expression }

6. Expressions (with precedence)
   - Expression: Relation { ( and | or | xor ) Relation }
   - Relation: Simple [ ( < | <= | > | >= | = | /= ) Simple ]
   - Simple: Factor { ( * | / | % ) Factor }
   - Factor: Summand { ( + | - ) Summand }
   - Summand: Primary | ( Expression )
   - Primary: [ Sign | not ] IntegerLiteral | [ Sign ] RealLiteral | true | false | ModifiablePrimary | RoutineCall
   - Sign: + | -
   - ModifiablePrimary: Identifier { . Identifier | [ Expression ] }

7. Body
   - Body: { SimpleDeclaration | Statement }

Additional Rules from Spec:
- Declarations separated by newlines or semicolons (not shown in grammar for readability)
- Scopes: entities visible from declaration point to end of containing block
- Forward declarations: routines can be declared without body, implemented later
- Array indexing: 1-based, dynamic expressions allowed
- Type conformance: strict for user-defined types, conversions for primitives
- Loop variables: implicitly integer, read-only in for loops

====================================================================
SYNTAX ANALYSIS THEORY & IMPLEMENTATION GUIDELINES
--------------------------------------------------------------------
- **Purpose**: Analyze token sequence according to grammar rules, build abstract syntax tree (AST), detect syntax errors
- **Parsing Technique**: Recursive descent parser (manual implementation, no parser generators)
- **Grammar Classes**: LL(1) compatible grammar with proper factorization and left-recursion elimination
- **AST Construction**: Build tree representation with nodes for each grammar construct
- **Error Recovery**: Panic-mode recovery, skip to synchronization points (statement/declaration boundaries)
- **Symbol Tables**: Maintain scopes for declarations, check forward references
- **Type Checking**: Basic syntactic type conformance (full semantic checking separate)
- **Integration**: Consume tokens from lexer via nextToken(), handle EOF appropriately

Technology Stack (from project details):
- Implementation: Java
- Lexer Integration: Use existing Lexer class
- Target: WebAssembly (WASM)

====================================================================
REQUIRED JAVA API (can be modified if needed)
--------------------------------------------------------------------
interface ASTNode {
    // Base interface for all AST nodes
    String toString(); // For debugging/tree printing
}

class Program implements ASTNode {
    List<Declaration> declarations;
}

abstract class Declaration implements ASTNode {}
class VariableDeclaration extends Declaration {
    String name;
    Type type; // null if inferred
    Expression initializer; // null if not specified
}
class TypeDeclaration extends Declaration {
    String name;
    Type type;
}
class RoutineDeclaration extends Declaration {
    String name;
    List<Parameter> parameters;
    Type returnType; // null for procedures
    RoutineBody body; // null for forward declarations
}

abstract class Type implements ASTNode {}
class PrimitiveType extends Type { String typeName; } // "integer", "real", "boolean"
class ArrayType extends Type {
    Expression size; // null for sizeless parameters
    Type elementType;
}
class RecordType extends Type {
    List<VariableDeclaration> fields;
}
class NamedType extends Type { String typeName; }

class Parameter {
    String name;
    Type type;
}

abstract class RoutineBody implements ASTNode {}
class FullBody extends RoutineBody {
    List<ASTNode> declarationsAndStatements; // Declaration or Statement
}
class ExpressionBody extends RoutineBody {
    Expression expression;
}

abstract class Statement implements ASTNode {}
class AssignmentStatement extends Statement {
    ModifiablePrimary target;
    Expression value;
}
class RoutineCallStatement extends Statement {
    String routineName;
    List<Expression> arguments;
}
class WhileLoopStatement extends Statement {
    Expression condition;
    List<ASTNode> body; // Declaration or Statement
}
class ForLoopStatement extends Statement {
    String loopVariable;
    Range range;
    boolean reverse;
    List<ASTNode> body; // Declaration or Statement
}
class IfStatement extends Statement {
    Expression condition;
    List<ASTNode> thenBody;
    List<ASTNode> elseBody; // null if no else
}
class PrintStatement extends Statement {
    List<Expression> expressions;
}

class Range {
    Expression start;
    Expression end; // null for array iteration
}

abstract class Expression implements ASTNode {}
class BinaryExpression extends Expression {
    Expression left;
    Expression right;
    String operator;
}
class UnaryExpression extends Expression {
    Expression operand;
    String operator;
}
class LiteralExpression extends Expression {
    String value; // String representation of literal
    String type; // "integer", "real", "boolean", "string"
}
class VariableExpression extends Expression {
    String name;
}
class FieldAccessExpression extends Expression {
    Expression record;
    String fieldName;
}
class ArrayAccessExpression extends Expression {
    Expression array;
    Expression index;
}
class RoutineCallExpression extends Expression {
    String routineName;
    List<Expression> arguments;
}

class ModifiablePrimary implements ASTNode {
    String baseName;
    List<Access> accesses; // Chain of .field or [index]
}

class Access {
    boolean isField; // true for .field, false for [index]
    String fieldName; // for field access
    Expression index; // for array access
}

class Parser {
    private Lexer lexer;
    private Token currentToken;
    Parser(Lexer lexer);
    Program parse() throws ParserException; // Main entry point
    private void advance() throws ParserException;
    private boolean match(TokenType expected);
    private void expect(TokenType expected) throws ParserException;
    // Parsing methods for each grammar rule
    private Program parseProgram() throws ParserException;
    private Declaration parseDeclaration() throws ParserException;
    private VariableDeclaration parseVariableDeclaration() throws ParserException;
    private TypeDeclaration parseTypeDeclaration() throws ParserException;
    private RoutineDeclaration parseRoutineDeclaration() throws ParserException;
    private Type parseType() throws ParserException;
    private Statement parseStatement() throws ParserException;
    private Expression parseExpression() throws ParserException;
    // ... additional private parsing methods
}

class ParserException extends Exception {
    String message;
    int line;
    int column;
    TokenType expected; // Optional: what token was expected
    TokenType found;    // Optional: what token was found
}

Constraints:
- Pure Java 8+ standard library (no external dependencies)
- Recursive descent implementation (no parser generator tools)
- Proper error recovery and informative error messages
- Build complete AST for all valid programs
- Handle all grammar rules from specification
- Integrate seamlessly with existing lexer

====================================================================
TEST SUITE (programs must parse without syntax errors)
--------------------------------------------------------------------
Test 1: Variable Declarations
var x: integer is 42;
var y: real is 3.14;
var flag: boolean is true;
var name is "test";

Test 2: Arrays & Data Structures
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];

Test 3: Record Types
type Point is record
    var x: real;
    var y: real;
end
var p1: Point;
p1.x := 1.5;
p1.y := 2.7;

Test 4: While Loops
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end

Test 5: For Loops
for i in 1..10 loop
    print i * i;
end
for j in 10..1 reverse loop
    print j;
end

Test 6: Functions & Recursion
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end
var result: integer is factorial(5);

Test 7: Type Conversions
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
var converted: integer is true;

Test 8: Complex Data Structures
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

====================================================================
DELIVERABLES
--------------------------------------------------------------------
1. Full, compilable Java code implementing the recursive descent parser
2. Brief README section in code comments: For each test, describe successful AST construction and confirm no ParserException
3. Integration Note: Parser produces AST compatible with semantic analyzer and code generator for WASM target

Focus on clean, modular code with detailed comments for each parsing method. Return only the code and README—no extra text.
