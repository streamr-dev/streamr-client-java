package com.streamr.client.protocol.control_layer;

public class ResendResponse {

    private String stream;
    private int partition;
    private String sub;

    public String getStream() {
        return stream;
    }

    public int getPartition() {
        return partition;
    }

    public String getSub() {
        return sub;
    }
}
