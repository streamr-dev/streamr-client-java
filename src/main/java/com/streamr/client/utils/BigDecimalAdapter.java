package com.streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalAdapter extends JsonAdapter<BigDecimal> {
    @Override
    public BigDecimal fromJson(JsonReader reader) throws IOException {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull();
        return new BigDecimal(reader.nextString());
    }

    @Override
    public void toJson(JsonWriter writer, BigDecimal value) throws IOException {
        if(value == null){
            writer.value("null");
        }else {
            writer.value(value.doubleValue());
        }
    }
}
