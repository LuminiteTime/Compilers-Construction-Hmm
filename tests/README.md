# Testing module

All tests are now in this module, under structured directories per component. Tests run in Docker, with compact, colored output.

## Structure

- `cases/`
  - `analyzer/` — analyzer tests, organized by theme subfolders (e.g., `precedence/`, `control_flow/`, `optimizer/`, `records/`, `arrays/`, `routines/`, `typecheck/`, `print/`). Each test is `<name>.i` with an optional `<name>.meta` file.
  - `parser/` — parser-only tests, also organized by theme (e.g., `basics/`, `precedence/`, `errors/`). Same file convention applies.
  - `.i`: input program in I language
  - `.meta`: expectations
    - `parseErr=0|1` — whether a non-zero exit code is acceptable
    - `expect: <substring>` — one or more lines with expected substrings in output
- `harness/run.sh` — discovers tests under `cases/**/**/*.i`, runs them via the native parser, checks expectations, prints a summary.

## Run (Docker)

```bash
bash ./docker_test.sh
```

This will:

- Build the image
- Build the native parser
- Run all `cases/*/*.i` tests with expectations from `.meta`
- Run Java tests in `tests:test`
 - Run Java tests in `tests:test`

### Verbose output

To print the program output (e.g., AST, diagnostics) for every test, enable verbose mode:

```bash
bash ./docker_test.sh --verbose
```

You can also run the harness directly with verbose output:

```bash
bash tests/harness/run.sh --verbose

### Suite/filter in Docker

You can pass the same selection flags through Docker:

```bash
bash ./docker_test.sh --suite analyzer
bash ./docker_test.sh --filter "range"
``` 

To run only the Java lexer tests in Docker, use the `lexer` suite:

```bash
bash ./docker_test.sh --suite lexer
```

When `--suite` is set to `parser` or `analyzer`, the Java lexer tests are skipped.
```

## Add a test

1. Choose a suite (directory under `cases/`), e.g. `analyzer` or `parser`, and a theme subfolder (e.g., `precedence/`).
2. Add `cases/<suite>/<theme>/<name>.i` with the input.
3. Add `cases/<suite>/<theme>/<name>.meta` with expectations:

```
parseErr=0
expect: Optimizations applied:
expect: IntegerLiteral: 8
```

4. Run `bash ./docker_test.sh`.

## Filter

Optional (locally or in Docker):

```bash
bash tests/harness/run.sh --suite analyzer
bash tests/harness/run.sh --filter "range"
```

## Notes

- All tests must live under `tests/cases/**`.
- Keep test names descriptive. One test = one `.i` file with an optional `.meta`.
