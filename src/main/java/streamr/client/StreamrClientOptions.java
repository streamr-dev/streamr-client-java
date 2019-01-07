package streamr.client;

public class StreamrClientOptions {

    private String apiKey = null;
    private String websocketApiUrl = "wss://www.streamr.com/api/v1/ws";
    private String restApiUrl = "https://www.streamr.com/api/v1";
    private long connectionTimeoutMillis = 10 * 1000;

    public StreamrClientOptions() {}

    public StreamrClientOptions(String apiKey) {
        this.apiKey = apiKey;
    }

    public StreamrClientOptions(String apiKey, String websocketApiUrl, String restApiUrl) {
        this.apiKey = apiKey;
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
    }

    public String getWebsocketApiUrl() {
        return websocketApiUrl;
    }

    public void setWebsocketApiUrl(String websocketApiUrl) {
        this.websocketApiUrl = websocketApiUrl;
    }

    public String getRestApiUrl() {
        return restApiUrl;
    }

    public void setRestApiUrl(String restApiUrl) {
        this.restApiUrl = restApiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
}
