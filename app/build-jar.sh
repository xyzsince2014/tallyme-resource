#!/bin/bash

# common defensive idiom for bash scripts that makes the script fail earlier and more predictably on errors
set -euo pipefail

# Build a runnable JAR for the Ktor app using the Gradle wrapper.
# Usage:
#   ./build-jar.sh [profile]
# If a profile is provided, it will be passed as a -P property to Gradle.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GRADLE_ARGS=()
if [ $# -gt 1 ]; then
  echo "Usage: $0 [profile]"
  exit 1
fi

if [ $# -eq 1 ]; then
  PROFILE="$1"
  echo "Building Ktor jar with profile: $PROFILE"
  GRADLE_ARGS+=("-Pprofile=$PROFILE")
else
  echo "Building Ktor jar (no profile)..."
fi

# Run the Gradle wrapper to build the shadow (fat) JAR expected by the Docker build.
if [ -x "./gradlew" ]; then
  ./gradlew "${GRADLE_ARGS[@]}" clean shadowJar -x test --no-daemon
else
  # fallback to system gradle if wrapper not present (less recommended)
  gradle "${GRADLE_ARGS[@]}" clean shadowJar -x test
fi

# Ensure the expected fat jar was produced deterministically.
shopt -s nullglob
jars=(build/libs/*-all.jar)
shopt -u nullglob

if [ "${#jars[@]}" -eq 0 ]; then
  echo "ERROR: no fat jar produced under build/libs matching *-all.jar"
  exit 1
fi

if [ "${#jars[@]}" -ne 1 ]; then
  echo "ERROR: multiple fat jars produced under build/libs matching *-all.jar"
  printf ' - %s\n' "${jars[@]}"
  exit 1
fi

JAR_PATH="${jars[0]}"
echo "Built jar: $JAR_PATH"

# Success
exit 0
