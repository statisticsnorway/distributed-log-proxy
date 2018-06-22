package no.ssb.distributedlog;

import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        String host = "127.0.0.1";
        int port = 8008;
        String instanceId = "1";

        for (int i = 0; i < args.length; i++) {
            if ("--host".equals(args[i]) || "-h".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.format("Missing host value argument\n");
                    return;
                }
                host = args[++i];
                continue;
            }
            if ("--port".equals(args[i]) || "-p".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.format("MissClean ing port value argument\n");
                    return;
                }
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.format("Port argument is not a valid integer\n");
                    return;
                }
                continue;
            }
            if ("--instance".equals(args[i]) || "-i".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.format("Missing instance value argument\n");
                    return;
                }
                instanceId = args[++i];
                continue;
            }
        }

        DistributedLogClientWrapper distributedLogClientWrapper = new DistributedLogClientWrapper("inet!127.0.0.1:8000", instanceId);

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new ProxyHttpHandler(distributedLogClientWrapper))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.warn("ShutdownHook triggered...");
            server.stop();
            distributedLogClientWrapper.client.close();
            LOG.info("Server shut down cleanly!");
        }));

        server.start();
        LOG.info("Listening on {}:{}", host, port);

        long time = System.currentTimeMillis() - now;
        LOG.info("Server started in {}ms", time);
    }
}
