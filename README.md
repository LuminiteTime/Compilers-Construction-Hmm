# Team Presentation: Project I Compiler

## 1. Team Name
Hmm

## 2. Team Members
- Mikhail Trifonov
- Kirill Efimovich

## 3. Project Technology Stack
- **Source Language**: Imperative (I) – Based on Project I, an imperative language with features like variable declarations, types, subprograms, statements (assignments, loops, conditionals), and expressions.
- **Implementation Language**: Java
- **Parser Development Tool**: Bison-based parser
- **Target Platform**: WASM

## 4. Tests Suite

## Test 1: Basic Variable Declarations
```
var x: integer is 42;
var y: real is 3.14;
var flag: boolean is true;
var name is "test";
```
**Expected**: Successful parsing of variable declarations with explicit types and type inference.

## Test 2: Array Declaration and Access
```
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];
```
**Expected**: Proper array type declaration, element assignment, and access with 1-based indexing.

## Test 3: Record Type Definition and Usage
```
type Point is record
    var x: real;
    var y: real;
end

var p1: Point;
p1.x := 1.5;
p1.y := 2.7;
```
**Expected**: Successful record type declaration and member access using dot notation.

## Test 4: While Loop with Boolean Expression
```
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```
**Expected**: Correct while loop parsing with boolean condition and body execution.

## Test 5: For Loop with Range
```
for i in 1..10 loop
    print i * i;
end

for j in 10..1 reverse loop
    print j;
end
```
**Expected**: Proper for loop parsing with both forward and reverse iteration.

## Test 6: Function Declaration and Call
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
**Expected**: Function declaration with parameters, return type, recursive call, and proper return statement.

## Test 7: Type Conversion Assignment
```
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
var converted: integer is true;
```
**Expected**: Successful type conversions according to the assignment conformance rules.

## Test 8: Error Case - Invalid Type Assignment
```
var flag: boolean is 3.14;
```
**Expected**: Compilation error due to invalid real-to-boolean assignment.

## Test 9: Complex Expression with Operator Precedence
```
var result: integer is 2 + 3 * 4 - 1;
var comparison: boolean is (result > 10) and not (result = 15);
```
**Expected**: Correct parsing and evaluation respecting operator precedence (*, /, % before +, -; comparison operators; logical operators).

## Test 10: Array Iteration and Record Array
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
**Expected**: Successful parsing of array of records, member assignment, and array iteration in for loop.
