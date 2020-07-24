package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.utils.EncryptedGroupKey;
import com.streamr.client.utils.GroupKey;

import javax.annotation.Nullable;
import java.io.IOException;

class EncryptedGroupKeyAdapter extends JsonAdapter<EncryptedGroupKey> {
    @Nullable
    @Override
    public EncryptedGroupKey fromJson(JsonReader reader) throws IOException {
        reader.beginArray();
        String groupKeyId = reader.nextString();
        String encryptedGroupKey = reader.nextString();
        reader.endArray();
        return new EncryptedGroupKey(groupKeyId, encryptedGroupKey);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable EncryptedGroupKey value) throws IOException {
        writer.beginArray();
        writer.value(value.getGroupKeyId());
        writer.value(value.getEncryptedGroupKeyHex());
        writer.endArray();
    }
}
