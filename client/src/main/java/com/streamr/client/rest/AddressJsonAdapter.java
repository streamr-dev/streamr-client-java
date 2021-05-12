package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.ethereum.common.Address;
import java.io.IOException;

public class AddressJsonAdapter extends JsonAdapter<Address> {
  @Override
  public Address fromJson(JsonReader reader) throws IOException {
    return new Address(reader.nextString());
  }

  @Override
  public void toJson(JsonWriter writer, Address value) throws IOException {
    writer.value(value.toString());
  }
}
