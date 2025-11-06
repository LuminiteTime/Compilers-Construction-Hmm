# Running Compiler Tests via Docker

This project includes a containerized test environment so tests run identically across Windows, macOS, and Linux.

## Prerequisites

- Docker Desktop (Windows/macOS) or Docker Engine (Linux)
- No need to install GCC, Make, Bison, Flex, or JDK locally

## One-command run

From the project root:

```bash
./docker_test.sh
```

What this does:
- Builds a Docker image with build tools (gcc/g++, make), bison, flex, and JDK 21
- Mounts the current workspace into the container
- Runs the unified test harness (`tests/harness/run.sh`) and Java tests inside the container

You can filter inside Docker too:

```bash
# Only analyzer suite
bash ./docker_test.sh --suite analyzer

# Filter tests by name pattern
bash ./docker_test.sh --filter "range"

# Combine with verbose output
bash ./docker_test.sh --suite analyzer --verbose

# Run only Java lexer tests
bash ./docker_test.sh --suite lexer
```

## Notes

- The C++ parser is rebuilt inside the container; any prebuilt binaries in your repo are cleaned first.
- The test script will auto-generate the JNI header (`compiler_lexer_Lexer.h`) if it is missing using `javac -h`.
- If you change code, just re-run `./docker_test.sh`; no need to rebuild the image unless Dockerfile changes.

## Troubleshooting

- If Docker complains about permissions on the mounted volume, run the script from a path Docker can access (e.g., inside your user directory) and ensure file sharing is enabled in Docker Desktop settings.
- If you are on WSL2, you can run the same command inside your WSL shell.
