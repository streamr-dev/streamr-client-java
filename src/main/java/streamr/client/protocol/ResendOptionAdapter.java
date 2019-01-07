package streamr.client.protocol;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendOptionAdapter extends JsonAdapter<ResendOption> {

    @Override
    public ResendOption fromJson(JsonReader reader) throws IOException {
        // TODO
        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void toJson(JsonWriter writer, ResendOption value) throws IOException {
        if (value.hasResendOption()) {
            if (value.getKey().equals(ResendOption.RESEND_ALL_KEY)) {
                writer.name(ResendOption.RESEND_ALL_KEY).value(true);
            } else {
                writer.name(value.getKey()).value(((Number) value.getValue()).longValue());
            }
        }
    }

}
