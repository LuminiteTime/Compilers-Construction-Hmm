# Analyzer Flow and Code Structure (English)

This document explains how the semantic analyzer runs end-to-end, the order of phases, and how each part of the code works. It is based on the implementation in `compiler/src/main/cpp/parser/analyzer.{h,cpp}` and the AST defined in `ast.h`.

## High-level flow

Input: a fully parsed `ProgramNode*` and a populated `SymbolTable`.
Output: diagnostics (errors/warnings) and an optimized AST that the existing printer shows.

The pipeline (Analyzer::analyze):

1) runChecks(root)
- Walks declarations and statements to perform non-mutating semantic validations.
- Detects type and shape errors (e.g., wrong field, wrong array index type, non-boolean conditions, assignment type mismatch, routine call issues, return type mismatch in arrow bodies).

2) runOptimizations(root) [only if no errors and optimizations enabled]
- Folds constant expressions in declarations and routines.
- Simplifies control flow:
  - If a condition becomes a constant, flatten the chosen branch.
  - While with constant false is removed.
- Hoists declarations from a chosen constant if-branch into the enclosing scope (program/body) with conflict detection.
- Immediately folds initializers of hoisted declarations.
- Removes unused declarations without initializers.

3) Post-optimization validation (safety check)
- Re-checks top-level statements and expressions against the set of global variable declarations.
- Reports undefined variable uses that can surface after dead-branch elimination.

The result is returned as `Analyzer::Result` with `errors`, `warnings`, and `optimizationsApplied`.

## Non-mutating checks (runChecks)

- Variable initializer type vs declared type
  - When a variable has an initializer, infer its type and verify it is compatible with the declared type. Otherwise: `error: Type mismatch in variable initializer: <name>`.

- Record types (duplicate fields)
  - Inside a `record` type declaration, duplicate field names are rejected: `error: Duplicate field '<name>' in type '<TypeName>'`.

- Routine declarations
  - Arrow-body (`=> expr`) functions: infer expression type and compare to declared return type. Mismatch yields `error: Routine '<name>' return type mismatch`.
  - Block-body (`is ... end`) functions: validate local declarations and statements within the body.

- Statements
  - Assignment: check target and value expressions; `error: Type mismatch in assignment` if types aren’t compatible.
  - While: condition must be boolean; otherwise `error: While condition must be boolean`.
  - For: 
    - Numeric range: both bounds must be integers.
    - For-in over arrays: the range expression must be an array type.
  - If: condition must be boolean; otherwise `error: If condition must be boolean`.
  - Print: validate all expressions in the list.
  - Routine call statements: existence, arity, type compatibility per parameter.

- Expressions
  - Array index must be integer: `error: Array index must be integer`.
  - Static bounds warning: if both index and size are constants and out-of-range, emit `warning: Array index <i> out of bounds [1..<N>] (static)`.
  - Record field access: ensure base is a record type and field exists; otherwise `error: Field access on non-record type` or `error: Unknown field '<field>' in record`.

## Optimizations (runOptimizations)

- Constant folding (foldExpression)
  - Arithmetic: +, -, *, /, mod (promotes to real where needed).
  - Comparisons: <, <=, >, >=, =, /= folded to booleans when operands are numeric constants.
  - Boolean ops: and, or, xor folded for boolean constants.
  - Unary ops: +, -, not folded for constant operands.

- If simplification with declaration hoisting
  - If a condition folds to a boolean literal, the analyzer selects the chosen body.
  - Before hoisting, it calls `simplifyInBody(chosen)` so nested constant branches are flattened first; this propagates inner declarations up into the chosen body’s declarations.
  - Then it hoists chosen-body declarations into the enclosing scope:
    - At program level: into `ProgramNode::declarations`.
    - At body level: into `BodyNode::declarations`.
  - Initializers of hoisted declarations are folded immediately.
  - Name conflicts (an existing variable with the same name in the same scope) produce `error: Duplicate variable declaration '<name>' in same scope` (no auto-renaming).
  - The chosen body’s statements are spliced into the current statement list (flattening the if).

- While false elimination
  - If a while condition folds to false, the loop is removed.

- Remove unused declarations
  - Gathers a set of used variables (from statements and from remaining initializers).
  - Drops declarations that are unused and have no initializer, counting it as an optimization.

## Post-optimization validation

- Collects global variable names from program-level declarations.
- Scans top-level statements and reports `error: Undefined variable '<name>'` if an identifier is referenced that isn’t declared at the program level.
- This step is intentionally top-level only to avoid false positives in non-constant branches.

## Error/warning accounting

- All errors and warnings are pushed into `Analyzer::Result` vectors.
- `optimizationsApplied` is incremented whenever a fold or structural simplification actually changes the AST.

## Ordering and rationale

- Checks precede optimizations to avoid transforming an invalid program.
- `simplifyInBody(chosen)` before hoisting ensures nested-if hoisting is correct and comprehensive (e.g., `y` declared in an inner constant-if won’t be missed).
- Post-optimization validation catches undefined references introduced by control simplifications.

## Extensibility

- Add new checks by expanding `checkNode`, `checkStatement`, and `checkExpression`.
- Add new optimizations by extending `foldExpression` or the `simplify*` passes.
- Keep the order: check → optimize → safety validation to maintain predictable semantics.

## Key functions map

- `Analyzer::analyze` — Orchestrates the whole pipeline.
- `runChecks` — Entry for all validations.
- `runOptimizations` — Constant folding and control simplification.
- `foldExpression` — Recursive expression folding engine.
- `simplifyInProgram` / `simplifyInBody` — If/while transformations and hoisting.
- `removeUnusedDeclarations` — Prunes unused variables (no initializer).
- Post-opt safety validator — Ensures top-level references are defined.
