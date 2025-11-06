# Semantic Analyzer for the Imperative (I) Language

This document describes the semantic analyzer implemented for the project, the checks it performs (non-mutating) and the optimizations it applies (AST-modifying), along with design notes, examples, and how to run it.

## Overview

- Location: `compiler/src/main/cpp/parser/analyzer.{h,cpp}`
- Integrated in: `parser.y` (runs automatically after successful parse)
- Input: C++ AST built by the Bison parser (`ast.h`)
- Output: Diagnostics to stdout (errors/warnings) and an optimized AST that is printed by the existing AST printer stub.
- Build integration: `compiler/src/main/cpp/parser/Makefile`

Why C++? The parser and AST types live in C++. A Java-only analyzer would require re-exposing or duplicating the AST. This analyzer operates directly on the existing C++ AST and symbol table.

## Contract

- Inputs: `ProgramNode*` root AST, symbol table populated during parsing.
- Outputs:
  - `errors[]`: semantic violations; compilation should fail if non-empty.
  - `warnings[]`: potential issues that don’t block compilation.
  - `optimizationsApplied`: count of AST transformations applied.
- Success criteria: no errors; warnings optional; AST may be simplified.

## Non-mutating semantic checks

1) Boolean conditions in control flow
- While: condition must be boolean
- If: condition must be boolean

2) Routine calls
- Existence: callee must be declared
- Arity: argument count must match
- Types: each argument must be assignment-compatible with parameter type

3) Record field access
- Access only valid on record-typed expressions
- Field must exist in the record type definition

4) Array indexing
- Index expression type must be integer
- Static bounds check when size is a constant: warns if constant index outside [1..N] (1-based, matching examples in docs)

5) Routine return (arrow body)
- If a routine has a declared return type and uses `=> expr` body, the expression type must be compatible with the declared return type

Notes
- Type inference delegates to `inferType` (`symbol.cpp`) to maintain consistency with parser-time type logic.
- Symbol table usage for routine/variable/type lookup is consistent with parser actions.

## Optimizations (AST-modifying)

1) Constant folding
- Binary arithmetic: `+ - * / mod` (promotes to real if needed)
- Comparisons: `< <= > >= = /=` folded to booleans when operands are constant numbers
- Boolean ops: `and or xor` folded on boolean constants
- Unary ops: `+ - not` folded on constants

2) If simplification
- If condition is constant true/false, replace the entire if node with the selected branch body (statements inlined)

Hoisting and folding of declarations
- When an if statement simplifies to a constant branch, any variable declarations inside the chosen branch are hoisted into the enclosing scope (program or body) to preserve semantics of flattened code.
- Initializers of hoisted declarations are folded immediately (e.g., `var b: integer is 1 + 2;` becomes `IntegerLiteral: 3`).
- Name conflicts created by hoisting result in a semantic error: `Duplicate variable declaration '<name>' in same scope` (no auto-renaming).

3) While false elimination
- Remove loops whose condition simplifies to constant `false`

4) Remove unused variable declarations
- Drops variable declarations that are never referenced and have no initializer (to avoid removing potential side effects)
- Applied both at program level and within bodies

Implementation notes
- Optimizations operate in a post-order manner to maximize folding opportunities
- The AST printer runs after optimizations, so changes are visible in the printed structure

## Examples

- Constant folding:
  - Input: `var a: integer is 5 + 3;`
  - Output AST shows `IntegerLiteral: 8`

- If simplification:
  - Input:
    ```
    if true then
      print 1
    end
    ```
  - Output AST contains only the `print 1` statement

- While false removal:
  - Input:
    ```
    while false loop
      print 1
    end
    ```
  - Output AST omits the loop entirely

- Routine call checks:
  - `x := add(1);` triggers an argument count mismatch if `add` expects two parameters

- Record field check:
  - `p.z` where `p: Point` and `Point` has only `x,y` → error

- Array checks:
  - `numbers[i]` where `i: real` → error
  - `numbers[4]` where `numbers: array[3] integer` → warning

## Running the analyzer

The analyzer runs automatically when invoking the native parser executable:

```bash
cd compiler/src/main/cpp/parser
make
./run_tests.sh           # runs the original 10 parser demos with analyzer
./run_analyzer_tests.sh  # runs analyzer-focused tests with assertions
```

In Docker (recommended for consistent toolchains):

```bash
bash ./docker_test.sh
```

This builds a container, runs `integration_test.sh`, which builds the parser, runs the original tests, then runs the analyzer tests.

## Test suite additions

Analyzer-focused tests live in `compiler/src/main/cpp/parser/`:
- `analyzer_const_and_control.i` – constant fold + if-true simplification + while-false removal
- `analyzer_routine_mismatch.i` – routine return type mismatch
- `analyzer_array_checks.i` – array index type error and static bounds warning
- `analyzer_record_field.i` – unknown record field access
- `analyzer_hoist_conflict.i` – hoisting causes duplicate declaration error
- `analyzer_hoist_and_fold.i` – hoist declarations from `if true` and fold their initializers
- `analyzer_hoist_nested.i` – nested `if true` bodies hoist and fold multiple declarations
- `analyzer_else_hoist_and_fold.i` – `if false` selects else-branch; declarations hoisted and folded
- `analyzer_while_false_nested.i` – loop body removed entirely when condition is `false`
- `analyzer_assignment_type_mismatch.i` – type mismatch in assignment is reported
- `analyzer_remove_unused_decl.i` – unused decl without initializer removed
- `analyzer_keep_decl_with_initializer.i` – unused decl with initializer is kept (initializer folded)
- `analyzer_postopt_undefined_top_level.i` – undefined variable after dead-branch removal reported
- `analyzer_boolean_folding.i` – boolean/unary/relational folding in initializers
- `analyzer_field_nonrecord.i` – field access on non-record expression reported
- `analyzer_if_condition_typecheck.i` – if condition must be boolean
- `analyzer_while_condition_typecheck.i` – while condition must be boolean
- `analyzer_record_field_duplicate.i` – duplicate record field names detected

Harness: `run_analyzer_tests.sh` asserts expected diagnostics and optimization evidence (e.g., folded literals, optimization count).

## Limitations and future work

- Type system is basic (mirrors current `inferType`); richer user-defined types and conversions can be added.
- Bounds checking is best-effort and static (constants only), which is typical for this phase.
- Function inlining is not implemented (non-trivial due to scoping/side effects); could be added as a future optimization for simple arrow routines.
- Exposing the analyzer to Java via JNI is straightforward if a Java API is desired (parse → analyze → return a formatted report string).

## Troubleshooting

- If you don’t see analyzer output, ensure `parser` was rebuilt and that `parser.y` includes `analyzer.h` and calls the analyzer in `WASMGenerator::generate`.
- On Windows, run under Docker or MSYS2 MinGW shell with `g++`, `bison`, and `flex` installed.