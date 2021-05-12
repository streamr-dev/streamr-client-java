package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.stream.EncryptedGroupKey;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

public class GroupKeyResponseAdapter extends AbstractGroupKeyMessageAdapter<GroupKeyResponse> {
  private final JsonAdapter<List<EncryptedGroupKey>> keyListAdapter =
      new Moshi.Builder()
          .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
          .add(EncryptedGroupKey.class, new EncryptedGroupKeyAdapter())
          .build()
          .adapter(Types.newParameterizedType(List.class, EncryptedGroupKey.class));

  GroupKeyResponseAdapter() {
    super(GroupKeyResponse.class);
  }

  @Nullable
  @Override
  public GroupKeyResponse fromJson(JsonReader reader) throws IOException {
    reader.beginArray();
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    List<EncryptedGroupKey> keys = keyListAdapter.fromJson(reader);
    reader.endArray();

    return new GroupKeyResponse(requestId, streamId, keys);
  }

  @Override
  public void toJson(JsonWriter writer, @Nullable GroupKeyResponse message) throws IOException {
    writer.beginArray();
    writer.value(message.getRequestId());
    writer.value(message.getStreamId());
    keyListAdapter.toJson(writer, message.getKeys());
    writer.endArray();
  }
}
