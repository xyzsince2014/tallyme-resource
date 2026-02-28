#!/bin/bash
docker container run -d --rm \
  --name tokyomap-resource \
  --net network_tokyomap \
  --ip 172.20.0.130 \
  --env-file dev.env \
  -p 8081:8081 \
  tokyomap.resource:dev
