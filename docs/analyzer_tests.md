# Analyzer Test Vectors and Expected Output

This document captures the analyzer-specific tests, their input programs, and the expected diagnostics/transformations.

Each test can be executed via the unified harness: `bash tests/harness/run.sh --suite analyzer` (also run by `docker_test.sh`).

## Test 1: Constant folding and control simplification

Input (`analyzer_const_and_control.i`):

```
var a: integer is 5 + 3;
if true then
  var z: integer is 1 + 2;
end
while false loop
  var w: integer is 10;
end
print a
```

Expected highlights:
- Optimizations applied: 3
  - `5 + 3` folded to `8`
  - `if true then ... end` body flattened (declaration moved/removed as no uses)
  - `while false loop ... end` removed entirely
- AST contains `IntegerLiteral: 8` for `a`'s initializer
- A single `PrintStatement` of `a` remains

## Test 2: Routine return type mismatch

Input (`analyzer_routine_mismatch.i`):

```
routine add(a: integer, b: integer): integer => a + b
routine f(a: integer): boolean => a + 1
var x: integer is 0;
x := add(1, 2);
```

Expected diagnostics:
- `error: Routine 'f' return type mismatch`

Notes:
- Argument arity/type checks are enforced at parse time; this test uses a correct call to `add` to avoid parser errors and let the analyzer report the return-type mismatch in `f`'s body.

## Test 3: Array index checks and static bounds

Input (`analyzer_array_checks.i`):

```
var numbers: array[3] integer;
var i: real is 1.0;
numbers[i] := 10;
numbers[4] := 20;
```

Expected diagnostics:
- `error: Array index must be integer`
- `warning: Array index 4 out of bounds [1..3] (static)`

## Test 4: Record field existence

Input (`analyzer_record_field.i`):

```
type Point is record
  var x: real;
  var y: real;
end
var p: Point;
var a: real is p.z;
```

Expected diagnostics:
- `error: Unknown field 'z' in record`
- The analyzer may also cascade a type mismatch error for `a`'s initializer depending on inference: `error: Type mismatch in variable initializer: a`

## Test 5: Hoisted declaration conflict

Input (`analyzer_hoist_conflict.i`):

```
var a: integer is 5 + 3;
if true then
  var a: integer is 1 + 2;
  print a;
end
print a
```

Behavior:
- The `if true` branch is flattened and its declarations are hoisted into the enclosing scope.
- Name conflicts are not auto-resolved; hoisting treats the declarations as being in the same scope.

Expected diagnostics:
- `error: Duplicate variable declaration 'a' in same scope`

---

## Test 6: Hoist and fold in then-branch

Input (`analyzer_hoist_and_fold.i`):

```
var a: integer is 5 + 3;
if true then
  var b: integer is 1 + 2;
  print b;
end
print a
```

Expected highlights:
- `b` is hoisted into the program scope and its initializer is folded to `3`
- AST shows `IntegerLiteral: 8` for `a` and `IntegerLiteral: 3` for `b`
- Selected branch statements are inlined (print `b` remains)

## Test 7: Nested hoisting with folding

Input (`analyzer_hoist_nested.i`):

```
if true then
  var x: integer is 1 + 1;
  if true then
    var y: integer is 2 + 2;
    print y;
  end
end
print x
```

Expected highlights:
- Both `x` and `y` are hoisted; initializers folded to `2` and `4`
- `print y` is preserved; `print x` at top level is valid

## Test 8: Else-branch hoist and fold

Input (`analyzer_else_hoist_and_fold.i`):

```
if false then
  var k: integer is 100;
else
  var c: integer is 2 + 2;
  print c;
end
```

Expected highlights:
- Else branch selected; `c` hoisted and folded to `4`

## Test 9: While false nested

Input (`analyzer_while_false_nested.i`):

```
var z: integer is 42;
while false loop
  var q: integer is 1 + 1;
  print q;
end
print z
```

Expected highlights:
- Loop removed entirely; no hoisting from dead loop body
- AST still contains `print z` and the declaration for `z`

## Test 10: Assignment type mismatch

Input (`analyzer_assignment_type_mismatch.i`):

```
var a: integer;
a := 1.0;
```

Expected diagnostics:
- `error: Type mismatch in assignment`

Notes:
- This violation is enforced at parse time; the parser prints a `Parse error ... Type mismatch in assignment`.
- The analyzer test harness treats the presence of that parse error text as a PASS for this test.

## Test 11: Remove unused declaration (no initializer)

Input (`analyzer_remove_unused_decl.i`):

```
var unusedVarUnique_1: integer;
print 0
```

Expected highlights:
- Unused declaration removed (no initializer → safe to drop)

## Test 12: Keep unused decl with initializer

Input (`analyzer_keep_decl_with_initializer.i`):

```
var keepMe: integer is 1 + 2;
```

Expected highlights:
- Declaration retained (has initializer); initializer folded to `3`

## Test 13: Undefined variable after dead-branch removal

Input (`analyzer_postopt_undefined_top_level.i`):

```
if false then
  var y: integer is 1;
end
print y
```

Expected diagnostics:
- `error: Undefined variable 'y'`

## Test 14: Boolean folding in initializer

Input (`analyzer_boolean_folding.i`):

```
var b: boolean is not (true and false) or (1 < 2);
```

Expected highlights:
- Initializer folded to `BooleanLiteral: true`

## Test 15: Field access on non-record

Input (`analyzer_field_nonrecord.i`):

```
var i: integer is 1;
var k: integer is i.z;
```

Expected diagnostics:
- `error: Field access on non-record type`

## Test 16: If condition must be boolean

Input (`analyzer_if_condition_typecheck.i`):

```
if 1 then
  print 1;
end
```

Expected diagnostics:
- `error: If condition must be boolean`

Notes:
- This is enforced at parse time; the parser prints a `Parse error ... If condition must be boolean`.
- The analyzer test harness treats the presence of that parse error text as a PASS for this test.

## Test 17: While condition must be boolean

Input (`analyzer_while_condition_typecheck.i`):

```
while 1 loop
  print 1;
end
```

Expected diagnostics:
- `error: While condition must be boolean`

Notes:
- This is enforced at parse time; the parser prints a `Parse error ... While condition must be boolean`.
- The analyzer test harness treats the presence of that parse error text as a PASS for this test.

## Test 18: Duplicate record field

Input (`analyzer_record_field_duplicate.i`):

```
type Point is record
  var x: real;
  var x: real;
end
```

Expected diagnostics:
- `error: Duplicate field 'x' in type 'Point'`

## Test 19: For-loop over numeric range (OK)

Input (`analyzer_for_range_ok.i`):

```
for i in 1 .. 3 loop
  print i;
end
```

Expected highlights:
- AST contains a `ForLoop` node and a `PrintStatement` inside the loop body.

## Test 20: For-loop numeric range type error

Input (`analyzer_for_range_type_error.i`):

```
for i in 1.0 .. 3 loop
end
```

Expected diagnostics:
- `error: For range bounds must be integers`

## Test 21: For-in over array (OK)

Input (`analyzer_for_in_array_ok.i`):

```
var arr: array[3] integer;
for i in arr loop
  print i;
end
```

Expected highlights:
- AST contains a `ForLoop` node and a `PrintStatement` inside the loop body.

## Test 22: Print with multiple expressions

Input (`analyzer_print_multiple.i`):

```
print 1, 2, 3;
```

Expected highlights:
- AST contains `PrintStatement` with three integer literals (`1`, `2`, `3`).

## Test 23: Routine call — undefined routine (parser-level)

Input (`analyzer_routine_call_undefined.i`):

```
foo(1);
```

Expected diagnostics:
- Parser prints `Parse error ... Undefined routine` (the analyzer harness treats this as PASS by checking for that text).

## Test 24: Routine call — arity mismatch (parser-level)

Input (`analyzer_routine_call_arity_mismatch.i`):

```
routine add(a: integer, b: integer): integer => a + b
add(1);
```

Expected diagnostics:
- Parser prints `Parse error ... Argument mismatch` (the analyzer harness treats this as PASS by checking for that text).

## Test 25: Routine call — parameter type mismatch (analyzer-level)

Input (`analyzer_routine_call_type_mismatch.i`):

```
routine f(a: integer, b: boolean): integer => a
var x: integer is 0;
f(1.0, 2);
```

Expected diagnostics:
- `error: Argument type mismatch in call to 'f' at position 1`
- `error: Argument type mismatch in call to 'f' at position 2`

Run locally (optional):

```
# In project root
bash ./docker_test.sh
# Or run analyzer tests directly via the harness
bash tests/harness/run.sh --suite analyzer
```

## Test 26: Arithmetic precedence and associativity

Input (`analyzer_precedence_arith.i`):

```
print 1 + 2 * 3;
print 1 * 2 + 3;
print 10 - 2 - 3;
print 10 / 2 * 3;
print 10 mod 6 mod 4;
print (1 + 2) * 3;
print 1 + (2 * 3);
```

Expected highlights:
- Folded outputs show: `7, 5, 5, 15, 0, 9, 7` respectively

## Test 27: Unary operator precedence

Input (`analyzer_precedence_unary.i`):

```
print -1 + 2 * 3;        # -> 5
print -(1 + 2) * 3;      # -> -9
print +1 * -2;           # -> -2
```

Expected highlights:
- Folded integer literals: `5, -9, -2`

## Test 28: Boolean operator precedence (not > and > xor > or)

Input (`analyzer_precedence_boolean.i`):

```
print true or false and false;      # -> true
print (true or false) and false;    # -> false
print not true or false;            # -> false
print not (true or false);          # -> false
print true xor false and false;     # -> true
```

Expected highlights:
- Folded boolean literals appear accordingly

## Test 29: Mixed arithmetic + boolean precedence

Input (`analyzer_precedence_mixed.i`):

```
print 1 + 2 > 3 + 5 * 2;                 # -> false
print 1 + 2 * 3 = 7 and not (2 * 2 = 5); # -> true
print 1 + 2 * 3 = 9 or 10 / 2 + 1 = 6;   # -> true
```

Expected highlights:
- Folded boolean literals: `false, true, true`
