package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.UnsupportedMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

public class ControlMessageAdapter extends JsonAdapter<ControlMessage> {
    private static final HashMap<Integer, ControlLayerAdapter<? extends ControlMessage>> adapters = new HashMap<>();

    public ControlMessageAdapter() {
        adapters.put(BroadcastMessage.TYPE, new BroadcastMessageAdapter());
        adapters.put(UnicastMessage.TYPE, new UnicastMessageAdapter());
        adapters.put(SubscribeResponse.TYPE, new SubscribeResponseAdapter());
        adapters.put(UnsubscribeResponse.TYPE, new UnsubscribeResponseAdapter());
        adapters.put(ResendResponseResending.TYPE, new ResendResponseResendingAdapter());
        adapters.put(ResendResponseResent.TYPE, new ResendResponseResentAdapter());
        adapters.put(ResendResponseNoResend.TYPE, new ResendResponseNoResendAdapter());
        adapters.put(ErrorResponse.TYPE, new ErrorResponseAdapter());
        adapters.put(PublishRequest.TYPE, new PublishRequestAdapter());
        adapters.put(SubscribeRequest.TYPE, new SubscribeRequestAdapter());
        adapters.put(UnsubscribeRequest.TYPE, new UnsubscribeRequestAdapter());
        adapters.put(ResendLastRequest.TYPE, new ResendLastRequestAdapter());
        adapters.put(ResendFromRequest.TYPE, new ResendFromRequestAdapter());
        adapters.put(ResendRangeRequest.TYPE, new ResendRangeRequestAdapter());
        adapters.put(DeleteRequest.TYPE, new DeleteRequestAdapter());
        adapters.put(DeleteResponse.TYPE, new DeleteResponseAdapter());
    }

    @Override
    public ControlMessage fromJson(JsonReader reader) throws IOException {
        reader.beginArray();
        int version = reader.nextInt();
        ControlMessage msg;
        if (version == ControlMessage.LATEST_VERSION) {
            int type = reader.nextInt();
            ControlLayerAdapter<? extends ControlMessage> adapter = adapters.get(type);
            if (adapter == null) {
                throw new UnsupportedMessageException("Unsupported control message type: "+type);
            }
            msg = adapter.fromJson(reader);
        } else {
            throw new UnsupportedMessageException("Unsupported control layer version: "+version);
        }
        reader.endArray();
        return msg;
    }

    @Override
    public void toJson(JsonWriter writer, ControlMessage value) throws IOException {
        ControlLayerAdapter<? extends ControlMessage> adapter = adapters.get(value.getType());
        adapter.controlMessagetoJson(writer, value);
    }
}
