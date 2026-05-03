# Language I Compiler

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-15%2B-orange)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36)](https://maven.apache.org/)

A compiler for **Language I**: lexing and parsing (JavaCC), semantic analysis, and code generation to **WebAssembly text format (WAT)**. The CLI can compile sources, dump an AST, or compile and execute via [wasmtime](https://wasmtime.dev/).

## Prerequisites

| Requirement | Purpose |
|-------------|---------|
| **JDK 15+** | Compile and run the project |
| **Apache Maven 3.x** | Build, tests, JavaCC code generation |
| **`wasmtime` on `PATH`** | `run` subcommand and WebAssembly integration tests |

## Build

From the repository root:

```bash
./scripts/build.sh
```

This runs `mvn clean package -DskipTests` and produces:

`target/compiler-i-1.0.0.jar`

Alternatively:

```bash
mvn clean package
```

## CLI usage

```text
java -jar target/compiler-i-1.0.0.jar <command> <source.i> [options]
```

| Command | Description |
|---------|-------------|
| `compile` | Emit WAT for the source file (`-o` output path). |
| `run` | Compile to WAT and execute with `wasmtime`. |
| `ast` | Parse and print the abstract syntax tree. |

Examples:

```bash
java -jar target/compiler-i-1.0.0.jar compile tests/integration/array_sum.i -o output/array_sum.wat
java -jar target/compiler-i-1.0.0.jar run tests/integration/array_sum.i -o output/array_sum.wat
java -jar target/compiler-i-1.0.0.jar ast tests/integration/array_sum.i
```

## Testing

**Unit tests** (JUnit 5):

```bash
mvn test
```

**WebAssembly integration tests** (compile `.i` fixtures to WAT and run under `wasmtime`; requires a prior build):

```bash
./run_integration_wasm.sh
```

## Project layout (overview)

- **`grammar/`** — JavaCC grammar (`LanguageI.jj`)
- **`src/main/java/`** — lexer, parser adapter, AST, semantic passes, codegen, CLI entrypoint
- **`tests/integration/`** — Language I programs used by integration scripts

## License

This project is released under the [MIT License](LICENSE).
