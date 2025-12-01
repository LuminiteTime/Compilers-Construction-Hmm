# Language I Compiler

## Overview

| Aspect                     | Value                                     |
|----------------------------|-------------------------------------------|
| Source language            | Language I                                |
| Implementation language    | Java                                      |
| Parser Technology          | JavaCC                                    |
| Target platform            | WebAssembly                               |

## Build

```bash
./scripts/build.sh
```

This produces a JAR at `target/compiler-i-1.0.0.jar`.

## CLI usage

General form:

```bash
java -jar target/compiler-i-1.0.0.jar <command> <source.i> [options]
```

Supported commands:

- `compile` – compile a Language I source file to WebAssembly text (WAT).

  ```bash
  java -jar target/compiler-i-1.0.0.jar compile tests/integration/array_sum.i -o output/array_sum.wat
  ```

- `run` – compile to WAT and immediately run via `wasmtime`.

  ```bash
  java -jar target/compiler-i-1.0.0.jar run tests/integration/array_sum.i
  ```

- `ast` – parse and print the AST of a program.

  ```bash
  java -jar target/compiler-i-1.0.0.jar ast tests/integration/array_sum.i
  ```

## Integration tests

WebAssembly integration tests (Language I programs compiled to WAT and run under `wasmtime`):

```bash
./run_integration_wasm.sh
```
