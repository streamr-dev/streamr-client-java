package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.utils.EncryptedGroupKey;
import java.util.Date;
import java.util.List;

public abstract class AbstractGroupKeyMessageAdapter<T extends AbstractGroupKeyMessage>
    extends JsonAdapter<T> {
  private final Class<T> groupKeyMessageClass;
  protected final JsonAdapter<List<String>> listOfStrings;
  protected final JsonAdapter<List<EncryptedGroupKey>> keyListAdapter;

  {
    final Moshi.Builder builder = new Moshi.Builder();
    listOfStrings =
        builder
            .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
            .build()
            .<List<String>>adapter(Types.newParameterizedType(List.class, String.class));
    keyListAdapter =
        builder
            .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
            .add(EncryptedGroupKey.class, new EncryptedGroupKeyAdapter())
            .build()
            .<List<EncryptedGroupKey>>adapter(
                Types.newParameterizedType(List.class, EncryptedGroupKey.class));
  }

  AbstractGroupKeyMessageAdapter(final Class<T> groupKeyMessageClass) {
    this.groupKeyMessageClass = groupKeyMessageClass;
  }

  public String groupKeyMessageToJson(AbstractGroupKeyMessage value) {
    return toJson(groupKeyMessageClass.cast(value));
  }
}
