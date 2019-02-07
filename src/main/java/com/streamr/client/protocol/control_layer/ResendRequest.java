package com.streamr.client.protocol.control_layer;

public class ResendRequest extends WebsocketRequest {

    private static final String TYPE = "resend";

    private String stream;
    private int partition = 0;
    private String sub;
    private ResendOption resendOption;

    public ResendRequest(String stream, int partition, String subscriptionId, ResendOption resendOption) {
        super(TYPE);
        this.stream = stream;
        this.partition = partition;
        this.sub = subscriptionId;

        if (resendOption == null) {
            resendOption = ResendOption.createNoResendOption();
        } else {
            this.resendOption = resendOption;
        }
    }

    public String getStream() {
        return stream;
    }

    public int getPartition() {
        return partition;
    }

    public String getSubscriptionId() {
        return sub;
    }

    public ResendOption getResendOption() {
        return resendOption;
    }
}
