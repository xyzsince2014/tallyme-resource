#!/bin/bash
set -euo pipefail

docker image rm tokyomap.resource:dev || true
docker build -t tokyomap.resource:dev app
