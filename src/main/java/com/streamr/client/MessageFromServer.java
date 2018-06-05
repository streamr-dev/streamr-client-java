package com.streamr.client;

public class MessageFromServer {

    public enum Type {
        Broadcast,
        Unicast,
        Subscribed,
        Unsubscribed,
        Resending,
        Resent,
        NoResend,
        Error
    }

    private static Type[] typeByMessageTypeCode = Type.values();

    private int messageTypeCode;
    private String subscriptionId;
    private Object payload;

    public MessageFromServer(int messageTypeCode, String subscriptionId, Object payload) {
        this.messageTypeCode = messageTypeCode;
        this.subscriptionId = subscriptionId;
        this.payload = payload;
    }

    public int getMessageTypeCode() {
        return messageTypeCode;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Object getPayload() {
        return payload;
    }

    public Type getType() {
        return typeByMessageTypeCode[messageTypeCode];
    }

}
