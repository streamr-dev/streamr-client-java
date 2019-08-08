package subscription;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.OrderedMsgChain;

import java.util.ArrayDeque;
import java.util.Map;

public class CombinedSubscription extends Subscription {
    private BasicSubscription sub;
    private final ArrayDeque<StreamMessage> queue = new ArrayDeque<>();
    public CombinedSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, String> groupKeysHex,
                                long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, groupKeysHex, propagationTimeout, resendTimeout);
        MessageHandler wrapperHandler = new MessageHandler() {
            @Override
            public void onMessage(com.streamr.client.Subscription sub, StreamMessage message) {
                handler.onMessage(sub, message);
            }
            @Override
            public void done(com.streamr.client.Subscription s) {
                RealTimeSubscription realTime = new RealTimeSubscription(streamId, partition, handler, groupKeysHex, propagationTimeout, resendTimeout);
                realTime.setGapHandler(sub.getGapHandler());
                realTime.setLastMessageRefs(sub.getChains());
                while(!queue.isEmpty()) {
                    StreamMessage msg = queue.poll();
                    realTime.handleRealTimeMessage(msg);
                }
                sub = realTime;
            }
        };
        sub = new HistoricalSubscription(streamId, partition, wrapperHandler, resendOption, groupKeysHex, propagationTimeout, resendTimeout, msg -> {
            queue.push(msg);
            return null;
        });
    }

    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        sub.setGapHandler(gapHandler);
    }

    @Override
    public boolean isResending() {
        return sub.isResending();
    }

    @Override
    public void setResending(boolean resending) {
        sub.setResending(resending);
    }

    @Override
    public boolean hasResendOptions() {
        return sub.hasResendOptions();
    }

    @Override
    public ResendOption getResendOption() {
        return sub.getResendOption();
    }

    @Override
    public void startResend() {
        sub.startResend();
    }

    @Override
    public void endResend() throws GapDetectedException {
        sub.endResend();
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        sub.handleRealTimeMessage(msg);
    }

    @Override
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        sub.handleResentMessage(msg);
    }

    @Override
    public void clear() {
        sub.clear();
    }
}
