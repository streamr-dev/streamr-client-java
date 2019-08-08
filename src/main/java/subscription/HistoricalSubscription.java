package subscription;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;

import java.util.Map;
import java.util.function.Function;

public class HistoricalSubscription extends BasicSubscription {
    private ResendOption resendOption;
    private Function<StreamMessage, Void> onRealTimeMsg;
    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, String> groupKeysHex,
                                  long propagationTimeout, long resendTimeout, Function<StreamMessage, Void> onRealTimeMsg) {
        super(streamId, partition, handler, groupKeysHex, propagationTimeout, resendTimeout);
        this.resendOption = resendOption;
        this.onRealTimeMsg = onRealTimeMsg;
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, String> groupKeysHex,
                                  long propagationTimeout, long resendTimeout) {
        this(streamId, partition, handler, resendOption, groupKeysHex, propagationTimeout, resendTimeout, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, String> groupKeysHex) {
        super(streamId, partition, handler, groupKeysHex);
        this.resendOption = resendOption;
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        super(streamId, partition, handler);
        this.resendOption = resendOption;
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler) {
        super(streamId, partition, handler);
    }

    @Override
    public boolean isResending() {
        return true;
    }

    @Override
    public void setResending(boolean resending) {

    }

    @Override
    public boolean hasResendOptions() {
        return true;
    }

    @Override
    public ResendOption getResendOption() {
        return resendOption;
    }

    @Override
    public void startResend() {

    }

    @Override
    public void endResend() throws GapDetectedException {
        this.handler.done(null);
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        if (onRealTimeMsg != null) {
            onRealTimeMsg.apply(msg);
        }
    }
}
