#!/usr/bin/env bash

# stop any running distributedlog container
RUNNING_CONTAINER_ID=$(docker ps -q -f 'name=distributedlog')
if [ -n "$RUNNING_CONTAINER_ID" ]; then docker stop $RUNNING_CONTAINER_ID; fi

# remove zombie distributedlog container
ZOMBIE_CONTAINER_ID=$(docker ps -q -f 'name=distributedlog' -f 'status=exited' -f 'status=dead')
if [ -n "$ZOMBIE_CONTAINER_ID" ]; then docker rm $ZOMBIE_CONTAINER_ID; fi

# run distributed-log sandbox using docker
docker run --name distributedlog -d -p 7000:7000 -p 8000:8000 -p 8001:8001 -e DLOG_ROOT_LOGGER=INFO,R guosijie/distributedlog /distributedlog/bin/dlog local 7000

# Wait for distributedlog sandbox to complete initialization
sleep 3

# execute the write-proxy within the already running distributedlog container
docker exec -e WP_SHARD_ID=1 -e WP_SERVICE_PORT=8000 -e WP_STATS_PORT=8001 -e WP_NAMESPACE='distributedlog://127.0.0.1:7000/messaging/distributedlog' distributedlog /distributedlog/bin/dlog-daemon.sh start writeproxy

# create stream-1 and stream-2
echo Y | docker exec -i distributedlog /distributedlog/bin/dlog tool create -u distributedlog://127.0.0.1:7000/messaging/distributedlog -r stream- -e 1-2

# command to run multi-tail of streams
# ./distributedlog-tutorials/distributedlog-basic/bin/runner run org.apache.distributedlog.basic.MultiReader distributedlog://127.0.0.1:7000/messaging/distributedlog stream-1,stream-2

# command to test that it's possible to write records to write-proxy
#./distributedlog-tutorials/distributedlog-basic/bin/runner run org.apache.distributedlog.basic.ConsoleProxyWriter 'inet!127.0.0.1:8000' stream-1
