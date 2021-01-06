package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.*;
import com.streamr.client.utils.EncryptedGroupKey;
import com.streamr.client.utils.HttpUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class GroupKeyAnnounceAdapter extends AbstractGroupKeyMessageAdapter<GroupKeyAnnounce> {
    private static final Moshi MOSHI = HttpUtils.addDefaultAdapters.apply(new Moshi.Builder())
            .add(EncryptedGroupKey.class, new EncryptedGroupKeyAdapter())
            .build();

    JsonAdapter<List<EncryptedGroupKey>> keyListAdapter = MOSHI.adapter(Types.newParameterizedType(List.class, EncryptedGroupKey.class));

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
