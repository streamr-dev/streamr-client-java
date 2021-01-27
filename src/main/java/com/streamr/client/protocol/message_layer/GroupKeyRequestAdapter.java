package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

public class GroupKeyRequestAdapter extends AbstractGroupKeyMessageAdapter<GroupKeyRequest> {

  GroupKeyRequestAdapter() {
    super(GroupKeyRequest.class);
  }

  @Nullable
  @Override
  public GroupKeyRequest fromJson(JsonReader reader) throws IOException {
    reader.beginArray();
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    String rsaPublicKey = reader.nextString();
    List<String> groupKeyIds = listOfStrings.fromJson(reader);
    reader.endArray();

    return new GroupKeyRequest(requestId, streamId, rsaPublicKey, groupKeyIds);
  }

  @Override
  public void toJson(JsonWriter writer, @Nullable GroupKeyRequest message) throws IOException {
    writer.beginArray();
    writer.value(message.getRequestId());
    writer.value(message.getStreamId());
    writer.value(message.getPublicKey());
    listOfStrings.toJson(writer, message.getGroupKeyIds());
    writer.endArray();
  }
}
