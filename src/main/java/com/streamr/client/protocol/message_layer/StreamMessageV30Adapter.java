package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.MessageType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class StreamMessageV30Adapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LogManager.getLogger();
    private static final MessageIDAdapter msgIdAdapter = new MessageIDAdapter();
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            // version field has already been read in StreamMessageAdapter
            MessageID messageID = msgIdAdapter.fromJson(reader);
            MessageRef previousMessageRef = null;
            // Peek at the previousMessageRef, as it can be null
            if (reader.peek().equals(JsonReader.Token.NULL)) {
                reader.nextNull();
            } else {
                previousMessageRef = msgRefAdapter.fromJson(reader);
            }
            MessageType messageType = MessageType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String signature = null;
            if (signatureType != SignatureType.NONE) {
                signature = reader.nextString();
            } else {
                reader.nextNull();
            }

            return new StreamMessage(messageID, previousMessageRef, messageType, serializedContent, StreamMessage.ContentType.JSON, StreamMessage.EncryptionType.NONE, null, signatureType, signature);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage msg) throws IOException {
        throw new RuntimeException("Serializing to old version is not supported");
    }
}
