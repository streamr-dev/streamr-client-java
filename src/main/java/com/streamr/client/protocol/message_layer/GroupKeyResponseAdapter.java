package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.*;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.HttpUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class GroupKeyResponseAdapter extends AbstractGroupKeyMessageAdapter<GroupKeyResponse> {

    private static final Moshi MOSHI = HttpUtils.addDefaultAdapters.apply(new Moshi.Builder())
            .add(GroupKey.class, new GroupKeyAdapter())
            .build();

    JsonAdapter<List<GroupKey>> keyListAdapter = MOSHI.adapter(Types.newParameterizedType(List.class, GroupKey.class));

    @Nullable
    @Override
    public GroupKeyResponse fromJson(JsonReader reader) throws IOException {
        reader.beginArray();
        String requestId = reader.nextString();
        String streamId = reader.nextString();
        List<GroupKey> keys = keyListAdapter.fromJson(reader);
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
