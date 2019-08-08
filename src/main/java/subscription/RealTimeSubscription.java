package subscription;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;

import java.util.Map;

public class RealTimeSubscription extends BasicSubscription {
    private boolean resending = false;
    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, String> groupKeysHex,
                                long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, groupKeysHex, propagationTimeout, resendTimeout);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, String> groupKeysHex) {
        super(streamId, partition, handler, groupKeysHex);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler) {
        super(streamId, partition, handler);
    }

    @Override
    public boolean isResending() {
        return resending;
    }

    @Override
    public void setResending(boolean resending) {
        this.resending = resending;
    }

    @Override
    public boolean hasResendOptions() {
        return false;
    }

    @Override
    public ResendOption getResendOption() {
        return null;
    }

    @Override
    public void startResend() {
        resending = true;
    }

    @Override
    public void endResend() throws GapDetectedException {
        resending = false;
        this.handler.done(null);
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        orderingUtil.add(msg);
    }
}
