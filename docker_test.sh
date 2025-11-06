#!/usr/bin/env bash
set -euo pipefail

# Optional flags
VERBOSE_FLAG=""
SUITE_VAL=""
FILTER_VAL=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--verbose)
      VERBOSE_FLAG=" --verbose"; shift ;;
    --suite)
      SUITE_VAL="${2:-}"; shift 2 ;;
    --filter)
      FILTER_VAL="${2:-}"; shift 2 ;;
    *)
      echo "Unknown option: $1"; exit 2 ;;
  esac
done

# Build pass-through args for inner harness (quote-safe for single quotes)
SUITE_ARG=""
FILTER_ARG=""
SUITE_INNER_ASSIGN="SELECT_SUITE=''"
if [[ -n "$SUITE_VAL" ]]; then
  SUITE_ESC=${SUITE_VAL//\'/\'\\\'\'}
  SUITE_ARG=" --suite '$SUITE_ESC'"
  SUITE_INNER_ASSIGN="SELECT_SUITE='$SUITE_ESC'"
fi
if [[ -n "$FILTER_VAL" ]]; then
  FILTER_ESC=${FILTER_VAL//\'/\'\\\'\'}
  FILTER_ARG=" --filter '$FILTER_ESC'"
fi

IMAGE="hmm-compiler-test:latest"

echo "[1/2] Building Docker image: ${IMAGE}"
docker build -t "${IMAGE}" .

# Resolve host path in a Docker-friendly way across Windows Git Bash and Unix shells
HOST_PATH="${PWD}"
UNAME_S=$(uname -s || echo "")
if [[ "$UNAME_S" == MINGW* || "$UNAME_S" == MSYS* || "$UNAME_S" == CYGWIN* ]]; then
  # On Git Bash/MSYS, prefer Windows-style path for Docker volume mounts
  if command -v pwd >/dev/null 2>&1 && pwd -W >/dev/null 2>&1; then
    HOST_PATH=$(pwd -W)
  fi
fi

echo "[2/2] Running tests inside Docker container"
# Prevent MSYS from rewriting /app into a Windows path; set working dir explicitly
MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" \
docker run --rm -t \
  -v "${HOST_PATH}:/app" \
  -w "/app" \
  "${IMAGE}" \
  -lc "set -euo pipefail; \
    ${SUITE_INNER_ASSIGN}; \
    find . -type f -name '*.sh' -exec sed -i 's/\r$//' {} + ; \
    if [ -f ./gradlew ]; then sed -i 's/\r$//' ./gradlew || true; chmod +x ./gradlew || true; fi; \
  echo '[A] Unified harness: tests/harness/run.sh (directory-based)'; \
  bash ./tests/harness/run.sh${VERBOSE_FLAG}${SUITE_ARG}${FILTER_ARG}; \
    echo; echo '[B] Java lexer tests: tests:test'; \
  if [ -z \"\$SELECT_SUITE\" ] || [ \"\$SELECT_SUITE\" = \"lexer\" ]; then \
      export LD_LIBRARY_PATH=/app/compiler/src/main/cpp/parser; \
      echo \"LD_LIBRARY_PATH=\$LD_LIBRARY_PATH\"; \
      chmod +x ./gradlew; \
      ./gradlew --no-daemon tests:test -Djava.library.path=/app/compiler/src/main/cpp/parser; \
    else \
      echo \"Skipping Java lexer tests (suite=\$SELECT_SUITE)\"; \
    fi"
