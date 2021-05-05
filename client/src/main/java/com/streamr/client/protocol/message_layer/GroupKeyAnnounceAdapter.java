package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.utils.EncryptedGroupKey;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

public class GroupKeyAnnounceAdapter extends AbstractGroupKeyMessageAdapter<GroupKeyAnnounce> {
  GroupKeyAnnounceAdapter() {
    super(GroupKeyAnnounce.class);
  }

  @Nullable
  @Override
  public GroupKeyAnnounce fromJson(JsonReader reader) throws IOException {
    reader.beginArray();
    String streamId = reader.nextString();
    List<EncryptedGroupKey> keys = keyListAdapter.fromJson(reader);
    reader.endArray();

    return new GroupKeyAnnounce(streamId, keys);
  }

  @Override
  public void toJson(JsonWriter writer, @Nullable GroupKeyAnnounce message) throws IOException {
    writer.beginArray();
    writer.value(message.getStreamId());
    keyListAdapter.toJson(writer, message.getKeys());
    writer.endArray();
  }
}
