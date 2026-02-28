#!/bin/bash
set -euo pipefail

docker image rm tokyomap.resource:dev
docker build -t tokyomap.resource:dev app
