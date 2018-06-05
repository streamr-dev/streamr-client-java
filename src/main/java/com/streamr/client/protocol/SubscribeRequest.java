package com.streamr.client.protocol;

public class SubscribeRequest extends WebsocketRequest {

    private static final String TYPE = "subscribe";

    private String stream;
    private String authKey;
    private int partition = 0;

    public SubscribeRequest(String stream, String authKey) {
        super(TYPE);
        this.stream = stream;
        this.authKey = authKey;
    }

    public SubscribeRequest(String stream, int partition, String authKey) {
        super(TYPE);
        this.stream = stream;
        this.partition = partition;
        this.authKey = authKey;
    }

    public String getStream() {
        return stream;
    }

    public String getAuthKey() {
        return authKey;
    }

    public int getPartition() {
        return partition;
    }
}
