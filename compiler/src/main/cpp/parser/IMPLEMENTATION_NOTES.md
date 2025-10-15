# Implementation Notes

This document describes the AST structure and semantic validations for each test case.

## Test 1: Variable Declarations

**Input:**
```
var x: integer is 42;
var y: real is 3.14;
var flag: boolean is true;
var name is "test";
```

**AST Structure:**
- ProgramNode containing 4 VariableDeclarationNodes
- Each VariableDeclarationNode has name, type, and initializer expression
- For "var name is "test"", type is inferred as primitive (simplified)

**Semantic Validations:**
- Type inference for variables without explicit type
- Symbol table declarations for each variable
- Scope management (global scope)

## Test 2: Arrays & Data Structures

**Input:**
```
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];
```

**AST Structure:**
- VariableDeclarationNode for array with ArrayTypeNode(size_expr, element_type)
- AssignmentNodes for array element assignments
- BinaryOpNode(PLUS) for sum calculation with ArrayAccessNodes

**Semantic Validations:**
- Array type construction with size expression and element type
- Array access bounds checking (not implemented, stub)
- Type compatibility for assignment

## Test 3: Record Types

**Input:**
```
type Point is record
    var x: real;
    var y: real;
end
var p1: Point;
p1.x := 1.5;
p1.y := 2.7;
```

**AST Structure:**
- TypeDeclarationNode with RecordTypeNode containing RecordBodyNode
- RecordBodyNode with VariableDeclarationNodes for fields
- VariableDeclarationNode for p1 with type alias lookup
- AssignmentNodes with FieldAccessNodes

**Semantic Validations:**
- Type alias declaration and lookup
- Record field access validation (field existence)
- Type compatibility for field assignments

## Test 4: While Loops

**Input:**
```
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```

**AST Structure:**
- WhileLoopNode with condition (BinaryOpNode(GT)) and body
- BodyNode containing PrintStatementNode and AssignmentNode

**Semantic Validations:**
- Boolean type check for while condition
- Variable lookup in symbol table
- Scope entry for loop body

## Test 5: For Loops

**Input:**
```
for i in 1..10 loop
    print i * i;
end
for j in 10..1 reverse loop
    print j;
end
```

**AST Structure:**
- ForLoopNodes with RangeNode(start, end), reverse flag, and body
- BodyNodes with PrintStatementNodes

**Semantic Validations:**
- Loop variable implicit declaration (integer type)
- Range expression evaluation
- Reverse iteration logic

## Test 6: Functions & Recursion

**Input:**
```
routine add(a: integer, b: integer): integer => a + b
var result: integer is add(2, 3);
```

**AST Structure:**
- RoutineDeclarationNode with RoutineHeaderNode and RoutineBodyNode (arrow expression)
- VariableDeclarationNode with RoutineCallNode in initializer

**Semantic Validations:**
- Parameter type matching
- Return type specification
- Routine call argument count and type checking
- Symbol table routine declaration

## Test 7: Type Conversions

**Input:**
```
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
var converted: integer is true;
```

**AST Structure:**
- VariableDeclarationNodes with type conversions in initializers

**Semantic Validations:**
- Primitive type conversion rules (as per language spec table)
- Assignment type compatibility

## Test 8: Error Detection

**Input:**
```
var flag: boolean is 3.14;
var x: integer;
x := "string";
```

**Expected Errors:**
- Type mismatch: real literal to boolean variable
- Type mismatch: string literal to integer variable (but lexer doesn't have strings, so identifier)

**Semantic Validations:**
- Type checking in assignments and initializations
- Error reporting via yyerror()

## Test 9: Operator Precedence

**Input:**
```
var result: integer is 2 + 3 * 4 - 1;
var comparison: boolean is (result > 10) and not (result = 15);
```

**AST Structure:**
- BinaryOpNode tree respecting precedence: ((2 + (3 * 4)) - 1)
- Boolean expression with AND, NOT, GT, EQ

**Semantic Validations:**
- Operator precedence rules in grammar
- Type inference for expressions

## Test 10: Complex Data Structures

**Input:**
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

**AST Structure:**
- TypeDeclarationNode for Student record
- ArrayTypeNode with RecordTypeNode as element type
- Nested field access: ArrayAccessNode.FieldAccessNode
- ForLoopNode iterating over array elements

**Semantic Validations:**
- Nested type definitions
- Complex expression type checking
- Array iteration semantics

## General Notes

- All AST nodes inherit from base classes for polymorphism
- Symbol table manages scoping with enterScope/exitScope
- Type checking is implemented via utility functions
- Memory management is manual (new/delete not implemented)
- Error recovery uses yyerror with line numbers