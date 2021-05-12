package com.streamr.client.rest;

import com.squareup.moshi.Moshi;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import com.streamr.ethereum.common.Address;
import java.math.BigDecimal;
import java.util.Date;

final class Json {
  private Json() {}

  public static Moshi.Builder newMoshiBuilder() {
    return new Moshi.Builder()
        .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
        .add(BigDecimal.class, new BigDecimalAdapter().nullSafe())
        .add(Address.class, new AddressJsonAdapter().nullSafe())
        .add(new InstantJsonAdapter());
  }
}
