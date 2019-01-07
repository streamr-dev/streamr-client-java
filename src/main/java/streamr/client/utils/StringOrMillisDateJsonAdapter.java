package streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Formats dates as milliseconds since epoch, similarly to Date#getTime().
 */
public final class StringOrMillisDateJsonAdapter extends JsonAdapter<Date> {
    @Override public synchronized Date fromJson(JsonReader reader) throws IOException {
        // Peek at the value to determine its type
        if (reader.peek().equals(JsonReader.Token.NUMBER)) {
            return new Date(reader.nextLong());
        } else {
            return Date.from(Instant.parse(reader.nextString()));
        }
    }

    @Override public synchronized void toJson(JsonWriter writer, Date value) throws IOException {
        writer.value(String.valueOf(value.getTime()));
    }
}
