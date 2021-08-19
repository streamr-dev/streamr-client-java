package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.utils.Address;
import java.io.IOException;

public class MessageIdAdapter extends JsonAdapter<MessageId> {
  @Override
  public MessageId fromJson(JsonReader reader) throws IOException {
    reader.beginArray();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    long timestamp = reader.nextLong();
    long sequenceNumber = reader.nextLong();
    String publisherId = reader.nextString();
    String msgChainId = reader.nextString();
    reader.endArray();
    return new MessageId.Builder()
        .withStreamId(streamId)
        .withStreamPartition(streamPartition)
        .withTimestamp(timestamp)
        .withSequenceNumber(sequenceNumber)
        .withPublisherId(new Address(publisherId))
        .withMsgChainId(msgChainId)
        .createMessageId();
  }

  @Override
  public void toJson(JsonWriter writer, MessageId value) throws IOException {
    writer.beginArray();
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    writer.value(value.getTimestamp());
    writer.value(value.getSequenceNumber());
    writer.value(value.getPublisherId().toString());
    writer.value(value.getMsgChainId());
    writer.endArray();
  }
}
