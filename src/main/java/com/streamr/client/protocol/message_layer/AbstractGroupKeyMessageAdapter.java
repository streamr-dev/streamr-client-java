package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;

public abstract class AbstractGroupKeyMessageAdapter<T extends AbstractGroupKeyMessage> extends JsonAdapter<T> {
    public String groupKeyMessageToJson(AbstractGroupKeyMessage value) {
        return toJson((T) value);
    }
}
