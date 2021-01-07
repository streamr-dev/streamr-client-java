package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;

public abstract class AbstractGroupKeyMessageAdapter<T extends AbstractGroupKeyMessage>
    extends JsonAdapter<T> {
  private final Class<T> groupKeyMessageClass;

  AbstractGroupKeyMessageAdapter(final Class<T> groupKeyMessageClass) {
    this.groupKeyMessageClass = groupKeyMessageClass;
  }

  public String groupKeyMessageToJson(AbstractGroupKeyMessage value) {
    return toJson(groupKeyMessageClass.cast(value));
  }
}
