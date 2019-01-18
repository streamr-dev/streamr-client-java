package com.streamr.client.protocol;

public class SubscribeResponse {

    private String stream;
    private int partition;

    public String getStream() {
        return stream;
    }

    public int getPartition() {
        return partition;
    }

}
