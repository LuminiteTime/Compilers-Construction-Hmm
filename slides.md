---
marp: true
theme: default
paginate: true
header: 'Team Hmm - Project I Compiler'
---

<!-- _class: lead -->

# Team Hmm

**Project I Compiler**

---


# Team Members

**Mikhail Trifonov**

**Kirill Efimovich**


---

# Technology Stack

## Implementation Details

| Component | Technology |
|-----------|------------|
| **Source Language** | Imperative (I) |
| **Implementation Language** | Java |
| **Parser Development Tool** | Bison-based parser |
| **Target Platform** | WebAssembly (WASM) |

---

### Test 1: Variable Declarations

**Code Example:**
```java
var x: integer is 42;
var y: real is 3.14;
var flag: boolean is true;
var name is "test";
```

**✓ Expected:** Successful parsing with explicit types and type inference

---

### Test 2: Arrays & Data Structures

**Code Example:**
```java
var numbers: array[5] integer;
numbers[1] := 10;
numbers[2] := 20;
var sum: integer is numbers[1] + numbers[2];
```

**✓ Expected:** Array declaration, assignment, and 1-based indexing

---

### Test 3: Record Types

**Code Example:**
```java
type Point is record
    var x: real;
    var y: real;
end

var p1: Point;
p1.x := 1.5;
p1.y := 2.7;
```

**✓ Expected:** Record type definition and dot notation access

---

### Test 4: While Loops

**Code Example:**
```java
var counter: integer is 10;
while counter > 0 loop
    print counter;
    counter := counter - 1;
end
```

**✓ Expected:** Boolean condition evaluation and loop execution

---

### Test 5: For Loops

**Code Example:**
```java
for i in 1..10 loop
    print i * i;
end

for j in 10..1 reverse loop
    print j;
end
```

**✓ Expected:** Forward and reverse range iteration

---

### Test 6: Functions & Recursion

**Code Example:**
```java
routine factorial(n: integer): integer is
    if n <= 1 then
        return 1;
    else
        return n * factorial(n - 1);
    end
end

var result: integer is factorial(5);
```

**✓ Expected:** Function declaration, recursion, and return values

---

### Test 7: Type Conversions

**Code Example:**
```java
var i: integer is 42;
var r: real is i;
var b: boolean is 1;
var converted: integer is true;
```

**✓ Expected:** Assignment conformance rules for type casting

---


### Test 8: Error Detection

**Code Example:**
```java
var flag: boolean is 3.14;
```

**❌ Expected:** Compilation error - invalid real-to-boolean assignment

---

### Test 9: Operator Precedence

**Code Example:**
```java
var result: integer is 2 + 3 * 4 - 1;
var comparison: boolean is (result > 10) and not (result = 15);
```

**✓ Expected:** Correct precedence: `*` > `+`, logical operators

---

### Test 10: Complex Data Structures

**Code Example:**
```java
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

**✓ Expected:** Nested data structures and iteration


---

<!-- _class: lead -->

# Thank you for attention.

