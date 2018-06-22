# DistributedLog HTTP Proxy
Stateless proxy service for integration with Apache DistributedLog through a simple http/json api

## Quickstart

1. Start the distributed-log sandbox: `docker/run-sandbox.sh`
1. Build the project using Maven: `mvn clean install`
1. Start the proxy using Java 8: `java -jar target/distributedlog-http-proxy.jar`
1. Test write using curl: `curl -i -X PUT 127.0.0.1:8008/stream-1 --data-binary '@/path/to/my/data.bin'`

## Known issues

1. The sandbox seems to get broken after the distributedlog-http-proxy disconnects and need to be restarted
every time before the distributedlog-http-proxy is restarted. Whe workaround for now is to re-run the sandbox
script after the sandbox breaks.
