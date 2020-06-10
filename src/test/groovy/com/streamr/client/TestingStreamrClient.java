package com.streamr.client;

import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.Subscription;
import com.streamr.client.utils.UnencryptedGroupKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestingStreamrClient extends StreamrClient {

    List<StreamMessage> receivedStreamMessages = new ArrayList<>();

    public TestingStreamrClient(StreamrClientOptions options) {
        super(options);
    }

    @Override
    public Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption, Map<String, UnencryptedGroupKey> groupKeys, boolean isExplicitResend) {
        // Capture received StreamMessages
        MessageHandler loggingHandler = (sub, message) -> {
            receivedStreamMessages.add(message);
            handler.onMessage(sub, message);
        };
        return super.subscribe(stream, partition, loggingHandler, resendOption, groupKeys, isExplicitResend);
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

    public List<StreamMessage> getReceivedStreamMessages() {
        return receivedStreamMessages;
    }

}
