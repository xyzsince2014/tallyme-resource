#!/bin/bash

# common defensive idiom
set -euo pipefail

if [ $# -gt 1 ]; then
  echo "⚠️ Usage: $0 [profile]"
  exit 1
fi

# default profile = develop
TARGET_PROFILE=${1:-develop}
echo "🚀 Build with profile: ${TARGET_PROFILE}"

build() {
  # skip tests by `-x test`
  ./gradlew -P profile="$1" clean shadowJar -x test --no-daemon
  echo "🌟 build() completed."
}

# execute with validation on PROFILE
PROFILES=("develop" "production")
for PROFILE in "${PROFILES[@]}"; do
  if [ "$PROFILE" == "$TARGET_PROFILE" ]; then
    build "$TARGET_PROFILE"
    echo "✅ Done."
    exit 0
  fi
done

echo "🛑 Invalid profile: ${TARGET_PROFILE}. Allowed profiles: ${PROFILES[*]}"
exit 1
