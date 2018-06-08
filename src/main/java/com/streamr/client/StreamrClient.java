package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class StreamrClient {

    private static final String ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors";
    private static final Moshi MOSHI = new Moshi.Builder().build();
    private static final JsonAdapter<List<Contributor>> CONTRIBUTORS_JSON_ADAPTER = MOSHI.adapter(
            Types.newParameterizedType(List.class, Contributor.class));

    private StreamrClientOptions options;

    public StreamrClient() {
        options = new StreamrClientOptions();
    }

    public StreamrClient(String apiKey) {
        options = new StreamrClientOptions(apiKey);
    }

    public StreamrClient(StreamrClientOptions options) {
        this.options = options;
    }

    static class Contributor {
        String login;
        int contributions;
    }

    // TODO: remove
    public static void main(String... args) throws Exception {
        OkHttpClient client = new OkHttpClient();

        // Create request for remote resource.
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .build();

        // Execute the request and retrieve the response.
        try (Response response = client.newCall(request).execute()) {
            // Deserialize HTTP response to concrete type.
            ResponseBody body = response.body();
            List<Contributor> contributors = CONTRIBUTORS_JSON_ADAPTER.fromJson(body.source());

            // Sort list by the most contributions.
            Collections.sort(contributors, new Comparator<Contributor>() {
                @Override public int compare(Contributor c1, Contributor c2) {
                    return c2.contributions - c1.contributions;
                }
            });

            // Output list of contributors.
            for (Contributor contributor : contributors) {
                System.out.println(contributor.login + ": " + contributor.contributions);
            }
        }

    }

}
