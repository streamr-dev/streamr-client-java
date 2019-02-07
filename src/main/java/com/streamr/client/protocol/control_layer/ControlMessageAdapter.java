package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.UnsupportedMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ControlMessageAdapter extends JsonAdapter<ControlMessage> {

    private static final BroadcastMessageAdapter broadcastMessageAdapater = new BroadcastMessageAdapter();
    private static final UnicastMessageAdapter unicastMessageAdapater = new UnicastMessageAdapter();
    private static final SubscribeResponseAdapter subscribeResponseAdapter = new SubscribeResponseAdapter();
    private static final UnsubscribeResponseAdapter unsubscribeResponseAdapter = new UnsubscribeResponseAdapter();
    private static final ResendResponseResendingAdapter resendResponseResendingAdapter = new ResendResponseResendingAdapter();
    private static final ResendResponseResentAdapter resendResponseResentAdapter = new ResendResponseResentAdapter();
    private static final ResendResponseNoResendAdapter resendResponseNoResendAdapter = new ResendResponseNoResendAdapter();
    private static final ErrorResponseAdapter errorResponseAdapter = new ErrorResponseAdapter();
    private static final PublishRequestAdapter publishRequestAdapter = new PublishRequestAdapter();
    private static final SubscribeRequestAdapter subscribeRequestAdapter = new SubscribeRequestAdapter();
    private static final UnsubscribeRequestAdapter unsubscribeRequestAdapter = new UnsubscribeRequestAdapter();
    private static final ResendLastRequestAdapter resendLastRequestAdapter = new ResendLastRequestAdapter();
    private static final ResendFromRequestAdapter resendFromRequestAdapter = new ResendFromRequestAdapter();
    private static final ResendRangeRequestAdapter resendRangeRequestAdapter = new ResendRangeRequestAdapter();

    @Override
    public ControlMessage fromJson(JsonReader reader) throws IOException {
        reader.beginArray();
        int version = reader.nextInt();
        ControlMessage msg;
        if (version == ControlMessage.LATEST_VERSION) {
            int type = reader.nextInt();
            if (type == BroadcastMessage.TYPE) {
                msg = broadcastMessageAdapater.fromJson(reader);
            } else if (type == UnicastMessage.TYPE) {
                msg = unicastMessageAdapater.fromJson(reader);
            } else if (type == SubscribeResponse.TYPE) {
                msg = subscribeResponseAdapter.fromJson(reader);
            } else if (type == UnsubscribeResponse.TYPE) {
                msg = unsubscribeResponseAdapter.fromJson(reader);
            } else if (type == ResendResponseResending.TYPE) {
                msg = resendResponseResendingAdapter.fromJson(reader);
            } else if (type == ResendResponseResent.TYPE) {
                msg = resendResponseResentAdapter.fromJson(reader);
            } else if (type == ResendResponseNoResend.TYPE) {
                msg = resendResponseNoResendAdapter.fromJson(reader);
            } else if (type == ErrorResponse.TYPE) {
                msg = errorResponseAdapter.fromJson(reader);
            } else if (type == PublishRequest.TYPE) {
                msg = publishRequestAdapter.fromJson(reader);
            } else if (type == SubscribeRequest.TYPE) {
                msg = subscribeRequestAdapter.fromJson(reader);
            } else if (type == UnsubscribeRequest.TYPE) {
                msg = unsubscribeRequestAdapter.fromJson(reader);
            } else if (type == ResendLastRequest.TYPE) {
                msg = resendLastRequestAdapter.fromJson(reader);
            } else if (type == ResendFromRequest.TYPE) {
                msg = resendFromRequestAdapter.fromJson(reader);
            } else if (type == ResendRangeRequest.TYPE) {
                msg = resendRangeRequestAdapter.fromJson(reader);
            } else {
                throw new UnsupportedMessageException("Unsupported control message type: "+type);
            }
        } else {
            throw new UnsupportedMessageException("Unsupported control layer version: "+version);
        }
        reader.endArray();
        return msg;
    }

    @Override
    public void toJson(JsonWriter writer, ControlMessage value) throws IOException {
        throw new IOException("toJson can only be called by adapters of concrete subclasses of ControlMessage.");
    }
}
