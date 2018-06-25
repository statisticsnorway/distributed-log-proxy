package no.ssb.distributedlog;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class DistributedLogHttpProxyTest {

    // Restart the docker sandbox locally every time before running this test.
    @Ignore
    @Test
    public void thatServerRespondsToWrite() throws JSONException {
        Main main = new Main("127.0.0.1", 8765, "test-1", "test-stream-1").start();
        String jsonResponse = new ThinProxyClient("http://127.0.0.1:8765/", "test-stream-1").write("Some unit-test data!");
        JSONAssert.assertEquals(jsonResponse, "{\"DLSN\":{\"base64\":\"AQAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAA==\"}}", true);
        main.stop();
    }
}
