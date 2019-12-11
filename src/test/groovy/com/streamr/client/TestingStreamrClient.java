package com.streamr.client;

import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.rest.UserInfo;

public class TestingStreamrClient extends StreamrClient {
    public TestingStreamrClient(StreamrClientOptions options) {
        super(options);
    }

    @Override
    public UserInfo getUserInfo() {
        return new UserInfo("name", "username", "id");
    }

    @Override
    public String getSessionToken() {
        return "sessionToken";
    }

    public void receiveMessage(ControlMessage msg) {
        handleMessage(msg.toJson());
    }

    public String getSubId(String streamId, int streamPartition) {
        try {
            return subs.get(streamId, streamPartition).getId();
        } catch (SubscriptionNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
