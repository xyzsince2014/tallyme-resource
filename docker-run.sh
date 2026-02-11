#!/bin/bash
docker container run -d --rm \
  --env-file $(pwd)/dev.env \
  --name tokyomap-resource \
  --net network_tokyomap \
  --ip 192.168.56.130 \
  tokyomap.resource:dev 
