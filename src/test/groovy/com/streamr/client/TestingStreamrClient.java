package com.streamr.client;

import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.ControlMessage;

import java.util.LinkedList;

public class TestingStreamrClient extends StreamrClient {
    private final LinkedList<String> sent = new LinkedList<>();
    public TestingStreamrClient(StreamrClientOptions options) {
        super(options);
    }

    public void receiveMessage(ControlMessage msg) {
        handleMessage(msg.toJson());
    }

    @Override
    protected void onSendMessage(String message) {
        sent.add(message);
    }

    public boolean noOtherMessagesSent() {
        boolean res = sent.isEmpty();
        if (!res) {
            System.out.println("Unexpected sent messages:");
            for (String msg: sent) {
                System.out.println(msg);
            }
        }
        return res;
    }

    public boolean expectToBeSent(ControlMessage msg) {
        String received = sent.poll();
        String expected = msg.toJson();
        boolean res = received.equals(expected);
        if (!res) {
            System.out.println("Expected: "+expected);
            System.out.println("But sent: "+received);
        }
        return res;
    }

    public String getSubId(String streamId, int streamPartition) {
        try {
            return subs.get(streamId, streamPartition).getId();
        } catch (SubscriptionNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
