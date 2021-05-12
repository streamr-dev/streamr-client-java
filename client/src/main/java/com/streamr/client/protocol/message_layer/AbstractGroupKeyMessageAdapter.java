package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.List;

public abstract class AbstractGroupKeyMessageAdapter<T extends AbstractGroupKeyMessage>
    extends JsonAdapter<T> {
  private final Class<T> groupKeyMessageClass;
  protected final JsonAdapter<List<String>> listOfStrings;
  protected final JsonAdapter<List<EncryptedGroupKey>> keyListAdapter;

  {
    final Moshi.Builder builder =
        new Moshi.Builder().add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe());

    final ParameterizedType listOfStringsAdapterType =
        Types.newParameterizedType(List.class, String.class);
    listOfStrings = builder.build().adapter(listOfStringsAdapterType);

    final ParameterizedType listOfEncryptedGroupKeysAdapterType =
        Types.newParameterizedType(List.class, EncryptedGroupKey.class);
    keyListAdapter =
        builder
            .add(EncryptedGroupKey.class, new EncryptedGroupKeyAdapter())
            .build()
            .adapter(listOfEncryptedGroupKeysAdapterType);
  }

  AbstractGroupKeyMessageAdapter(final Class<T> groupKeyMessageClass) {
    this.groupKeyMessageClass = groupKeyMessageClass;
  }

  public String groupKeyMessageToJson(AbstractGroupKeyMessage value) {
    return toJson(groupKeyMessageClass.cast(value));
  }
}
