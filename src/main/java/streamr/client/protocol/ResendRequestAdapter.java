package streamr.client.protocol;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendRequestAdapter extends JsonAdapter<ResendRequest> {

    @Override
    public ResendRequest fromJson(JsonReader reader) throws IOException {
        // TODO
        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void toJson(JsonWriter writer, ResendRequest value) throws IOException {
        writer.beginObject();

        writer.name("stream").value(value.getStream());
        writer.name("partition").value(value.getPartition());
        writer.name("sub").value(value.getSubscriptionId());

        new ResendOptionAdapter().toJson(writer, value.getResendOption());

        writer.endObject();
    }
}
