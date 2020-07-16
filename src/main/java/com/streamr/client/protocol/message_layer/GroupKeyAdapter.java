package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.utils.GroupKey;

import javax.annotation.Nullable;
import java.io.IOException;

class GroupKeyAdapter extends JsonAdapter<GroupKey> {
    @Nullable
    @Override
    public GroupKey fromJson(JsonReader reader) throws IOException {
        reader.beginArray();
        String groupKeyId = reader.nextString();
        String groupKey = reader.nextString();
        reader.endArray();

        try {
            return new GroupKey(groupKeyId, groupKey);
        } catch (InvalidGroupKeyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable GroupKey value) throws IOException {
        writer.beginArray();
        writer.value(value.getGroupKeyId());
        writer.value(value.getGroupKeyHex());
        writer.endArray();
    }
}
