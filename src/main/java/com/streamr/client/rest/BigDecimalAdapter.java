package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalAdapter extends JsonAdapter<BigDecimal> {
  @Override
  public BigDecimal fromJson(JsonReader reader) throws IOException {
    return new BigDecimal(reader.nextString());
  }

  @Override
  public void toJson(JsonWriter writer, BigDecimal value) throws IOException {
    writer.value(value.doubleValue());
  }
}
