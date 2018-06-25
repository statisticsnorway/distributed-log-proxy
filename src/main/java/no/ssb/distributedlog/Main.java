package no.ssb.distributedlog;

import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Undertow server;
    private final DistributedLogClientWrapper distributedLogClientWrapper;

    public Main(String host, int port, String instanceId, String streamToCreateOnStartup) {
        distributedLogClientWrapper =
                new DistributedLogClientWrapper(
                        "inet!127.0.0.1:8000",
                        instanceId,
                        streamToCreateOnStartup);

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new ProxyHttpHandler(distributedLogClientWrapper))
                .build();
    }

    Main start() {
        server.start();
        LOG.info("Server started, listening on {}:{}",
                ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getHostString(),
                ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort());
        return this;
    }

    void stop() {
        server.stop();
        distributedLogClientWrapper.client.close();
        LOG.info("Server shut down");
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8008;
        String instanceId = "1";
        String streamToCreateOnStartup = null; // stream creation managed externally by default

        for (int i = 0; i < args.length; i++) {
            if ("--host".equals(args[i]) || "-h".equals(args[i])) {
                if (i + 1 >= args.length) {
                    LOG.error("Missing host value argument\n");
                    return;
                }
                host = args[++i];
                continue;
            }
            if ("--port".equals(args[i]) || "-p".equals(args[i])) {
                if (i + 1 >= args.length) {
                    LOG.error("MissClean ing port value argument\n");
                    return;
                }
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    LOG.error("Port argument is not a valid integer\n");
                    return;
                }
                continue;
            }
            if ("--instance".equals(args[i]) || "-i".equals(args[i])) {
                if (i + 1 >= args.length) {
                    LOG.error("Missing instance value argument\n");
                    return;
                }
                instanceId = args[++i];
                continue;
            }
            if ("--stream".equals(args[i]) || "-s".equals(args[i])) {
                if (i + 1 >= args.length) {
                    LOG.error("Missing stream creation value argument\n");
                    return;
                }
                streamToCreateOnStartup = args[++i];
                continue;
            }
        }

        Main main = new Main(host, port, instanceId, streamToCreateOnStartup);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> main.stop()));
        main.start();
    }
}
