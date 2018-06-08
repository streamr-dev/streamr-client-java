package com.streamr.client.protocol;

import com.squareup.moshi.*;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;

import java.io.IOException;

public class MessageFromServerAdapter extends JsonAdapter<MessageFromServer> {

    // Thread safe
    private static final Moshi moshi = new Moshi.Builder().build();
    private static final JsonAdapter[] adapterByCode = new JsonAdapter[] {
            new StreamMessageAdapter(), // 0: Broadcast
            new StreamMessageAdapter(), // 1: Unicast
            moshi.adapter(SubscribeResponse.class),   // 2: Subscribed
            moshi.adapter(UnsubscribeResponse.class), // 3: Unsubscribed
            moshi.adapter(ResendingMessage.class), // 4: Resending
            moshi.adapter(ResentMessage.class), // 5: Resent
            moshi.adapter(NoResendMessage.class), // 6: No resend
            moshi.adapter(String.class) // 7: Error
    };

    @Override
    public MessageFromServer fromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();
            if (version != 0) {
                throw new UnsupportedMessageException("Unrecognized message version: " + version);
            }

            int messageTypeCode = reader.nextInt();
            String subscriptionId = reader.nextString();

            // Parse payload
            if (messageTypeCode >= adapterByCode.length) {
                throw new UnsupportedMessageException("Unrecognized payload type: " + messageTypeCode);
            }
            Object payload = adapterByCode[messageTypeCode].fromJson(reader);

            reader.endArray();

            return new MessageFromServer(messageTypeCode, subscriptionId, payload);
        } catch (JsonDataException e) {
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        } finally {
            reader.close();
        }
    }

    @Override
    public void toJson(JsonWriter writer, MessageFromServer value) throws IOException {
        throw new RuntimeException("Unimplemented!"); // TODO
    }
}
