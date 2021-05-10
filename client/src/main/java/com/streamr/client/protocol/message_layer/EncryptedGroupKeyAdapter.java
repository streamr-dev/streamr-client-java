package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.utils.EncryptedGroupKey;

import javax.annotation.Nullable;
import java.io.IOException;

public class EncryptedGroupKeyAdapter extends JsonAdapter<EncryptedGroupKey> {

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
