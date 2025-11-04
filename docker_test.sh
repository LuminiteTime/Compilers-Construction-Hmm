#!/usr/bin/env bash
set -euo pipefail

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
  -lc "find . -type f -name '*.sh' -exec sed -i 's/\r$//' {} + ; bash ./integration_test.sh"
