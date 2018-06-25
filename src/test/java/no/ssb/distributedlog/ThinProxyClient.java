package no.ssb.distributedlog;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class ThinProxyClient {

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/octet-stream");
    private final String baseurl;
    private final URI uri;

    ThinProxyClient(String baseurl, String stream) {
        this.baseurl = baseurl.endsWith("/") ? baseurl : baseurl + "/";
        try {
            uri = new URI(this.baseurl + stream);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Response post(URL url, byte[] data) throws IOException {
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }

    String write(String data) {
        try {
            Response response = post(uri.toURL(), data.getBytes(StandardCharsets.UTF_8));
            if (response.code() < 200 || 300 <= response.code()) {
                throw new RuntimeException("Write to distributedlog-http-proxy failed. StatusCode: " + response.code());
            }
            String json = response.body().string();
            System.out.println(json);
            return json;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
