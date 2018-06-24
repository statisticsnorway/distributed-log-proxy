package no.ssb.distributedlog;

import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.apache.distributedlog.DLSN;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ProxyHttpHandler implements HttpHandler {

    private final DistributedLogClientWrapper clientWrapper;

    public ProxyHttpHandler(DistributedLogClientWrapper clientWrapper) {
        this.clientWrapper = clientWrapper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if (exchange.getRequestMethod().equalToString("get")) {
            get(exchange);
            return;
        } else if (exchange.getRequestMethod().equalToString("put")) {
            put(exchange);
            return;
        }

        exchange.setStatusCode(400);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Unsupported method: " + exchange.getRequestMethod());
    }

    private void get(HttpServerExchange exchange) {
        // TODO read data from distributedlog at DLSN provided in request-path and return in body to client
        exchange.setStatusCode(200);
    }

    private void put(HttpServerExchange exchange) {
        String requestPath = exchange.getRequestPath();
        String stream = requestPath.substring(1); // remove leading forward-slash
        if (stream.contains("/")) {
            exchange.setStatusCode(404);
            return;
        }
        exchange.getRequestReceiver().receiveFullBytes(onReceiveHttpBodyCallback(stream));
    }

    private Receiver.FullBytesCallback onReceiveHttpBodyCallback(String stream) {
        return (exchange, buf) -> {
            ByteBuffer data = ByteBuffer.wrap(buf);
            DLSN dlsn = clientWrapper.write(stream, data, 10, TimeUnit.SECONDS);
            exchange.setStatusCode(201);
            String serializedDLSN = dlsn.serialize();
            exchange.getResponseHeaders().put(new HttpString("Location"), "/" + stream + "/" + serializedDLSN);
            exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json; charset=utf-8");
            StringBuilder sb = new StringBuilder(30 + serializedDLSN.length());
            sb.append("{\"DLSN\":\"{\"base64\":\"").append(serializedDLSN).append("\"}}");
            ByteBuffer responseData = ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8));
            exchange.setResponseContentLength(responseData.limit());
            exchange.getResponseSender().send(responseData);
        };
    }


}
