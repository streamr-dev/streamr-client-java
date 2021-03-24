package com.streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class AddressJsonAdapter extends JsonAdapter<Address>{

    @Override 
    public synchronized Address fromJson(JsonReader reader) throws IOException {
        return new Address(reader.nextString());
    }

    @Override 
    public synchronized void toJson(JsonWriter writer, Address value) throws IOException {
        writer.value(value.toString());
    }

}