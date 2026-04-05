#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
docker container run -d --rm \
  --name tokyomap-resource \
  --net network_tokyomap \
  --ip 172.20.0.130 \
  --env-file "${SCRIPT_DIR}/dev.env" \
  -p 8081:8081 \
  tokyomap.resource:dev
