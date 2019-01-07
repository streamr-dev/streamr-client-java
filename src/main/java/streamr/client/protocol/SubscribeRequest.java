package streamr.client.protocol;

public class SubscribeRequest extends WebsocketRequest {

    private static final String TYPE = "subscribe";

    private String stream;
    private String sessionToken;
    private int partition = 0;

    public SubscribeRequest(String stream, String sessionToken) {
        super(TYPE);
        this.stream = stream;
        this.sessionToken = sessionToken;
    }

    public SubscribeRequest(String stream, int partition, String sessionToken) {
        super(TYPE);
        this.stream = stream;
        this.partition = partition;
        this.sessionToken = sessionToken;
    }

    public String getStream() {
        return stream;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public int getPartition() {
        return partition;
    }
}
