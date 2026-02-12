#!/bin/bash
docker container run -d --rm \
  --env-file $(pwd)/dev.env \
  --name tokyomap-resource \
  --net network_tokyomap \
  --ip 172.20.0.130 \
  tokyomap.resource:dev 
