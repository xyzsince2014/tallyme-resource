#!/bin/bash
docker container run -d \
  --env-file $(pwd)/dev.env \
  -p 8081:8081 \
  --rm \
  --name resource \
  --net network_tokyomap \
  --ip 192.168.56.130 \
  tokyomap.resource:dev 
