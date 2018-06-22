package no.ssb.distributedlog;

import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.thrift.ClientId;
import com.twitter.util.Duration;
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
import org.apache.distributedlog.DLSN;
import org.apache.distributedlog.service.DistributedLogClient;
import org.apache.distributedlog.service.DistributedLogClientBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DistributedLogClientWrapper {

    final DistributedLogClient client;

    DistributedLogClientWrapper(String finagleName, String instanceId) {
        ClientBuilder clientBuilder = ClientBuilder.get()
                .hostConnectionLimit(5)
                .hostConnectionCoresize(2)
                .tcpConnectTimeout(Duration.fromMilliseconds(500))
                .connectTimeout(Duration.fromMilliseconds(500))
                .requestTimeout(Duration.fromSeconds(5));

        client = DistributedLogClientBuilder.newBuilder()
                .clientBuilder(clientBuilder)
                .clientId(ClientId.apply("distributedlog-http-proxy-" + instanceId))
                .name("distributedlog-http-proxy-" + instanceId)
                .finagleNameStr(finagleName)
                .thriftmux(true)
                .build();
    }

    DLSN write(String stream, ByteBuffer data, long timeout, TimeUnit unit) {
        Future<DLSN> writeFuture = client.write(stream, data);
        AtomicReference<Throwable> failureCause = new AtomicReference<>();
        AtomicReference<DLSN> successResult = new AtomicReference<>();
        CountDownLatch writeCompleteSignal = new CountDownLatch(1);
        writeFuture.addEventListener(new FutureEventListener<DLSN>() {
            @Override
            public void onFailure(Throwable cause) {
                failureCause.set(cause);
                writeCompleteSignal.countDown();
            }

            @Override
            public void onSuccess(DLSN value) {
                successResult.set(value);
                writeCompleteSignal.countDown();
            }
        });
        try {
            if (!writeCompleteSignal.await(timeout, unit)) {
                throw new RuntimeException("Timed out while waiting on write to complete.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (failureCause.get() != null) {
            Throwable cause = failureCause.get();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
        if (successResult.get() != null) {
            System.out.println("DLSN: " + successResult.get().toString());
            return successResult.get();
        }
        throw new IllegalStateException("Neither success nor failure, impossible!");
    }
}
