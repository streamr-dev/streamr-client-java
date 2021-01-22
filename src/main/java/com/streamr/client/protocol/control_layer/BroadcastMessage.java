package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;
import java.util.Objects;

public final class BroadcastMessage extends ControlMessage {
    public static final int TYPE = 0;

    private final StreamMessage streamMessage;

    public BroadcastMessage(String requestId, StreamMessage streamMessage) {
        super(TYPE, requestId);
        this.streamMessage = streamMessage;
    }

    public StreamMessage getStreamMessage() {
        return streamMessage;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final BroadcastMessage that = (BroadcastMessage) obj;
        return getType() == that.getType() && Objects.equals(getRequestId(), that.getRequestId()) &&
            Objects.equals(streamMessage, that.streamMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamMessage, getType(), getRequestId());
    }

    @Override
    public String toString() {
        return String.format("BroadcastMessage{requestId=%s, streamMessage=%s",
                getRequestId(), streamMessage);
    }
}
