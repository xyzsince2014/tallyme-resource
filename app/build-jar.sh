#!/bin/bash

# common defensive idiom for bash scripts that makes the script fail earlier and more predictably on errors
set -euo pipefail

# Build a runnable JAR for the Ktor app using the Gradle wrapper.
# Usage:
#   ./build-jar.sh [profile]
# If a profile is provided, it will be passed as a -P property to Gradle.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROFILE_ARG=""
if [ $# -gt 1 ]; then
  echo "Usage: $0 [profile]"
  exit 1
fi

if [ $# -eq 1 ]; then
  PROFILE="$1"
  echo "Building Ktor jar with profile: $PROFILE"
  PROFILE_ARG="-P profile=$PROFILE"
else
  echo "Building Ktor jar (no profile)..."
fi

# Run the Gradle wrapper to build the project (skip tests to speed up builds)
if [ -x "./gradlew" ]; then
  ./gradlew ${PROFILE_ARG} clean build -x test --no-daemon
else
  # fallback to system gradle if wrapper not present (less recommended)
  gradle ${PROFILE_ARG} clean build -x test
fi

# Ensure a jar was produced
JAR_PATH=$(ls build/libs/*.jar 2>/dev/null | head -n1 || true)
if [ -z "$JAR_PATH" ]; then
  echo "ERROR: no jar produced under build/libs"
  exit 1
fi

echo "Built jar: $JAR_PATH"

# Success
exit 0
