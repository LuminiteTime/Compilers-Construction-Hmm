# Dev/test container for building and running the compiler tests
# Avoids OS-specific issues by standardizing on Ubuntu + toolchain

FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      build-essential \
      bison \
      flex \
      openjdk-21-jdk-headless \
      ca-certificates \
      git \
      bash \
 && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME for JNI header generation
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

# Default entrypoint is bash; we'll mount the repo at /app and run our script
ENTRYPOINT ["/bin/bash"]
