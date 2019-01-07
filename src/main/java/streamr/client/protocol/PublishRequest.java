package streamr.client.protocol;

import java.util.Date;

/*
{
  "type": "publish",
  "stream": "streamId",
  "sessionToken": "sessionToken",
  "msg": "{}",                 // the message as stringified json
  "ts": 1533924184016,         // timestamp (optional), defaults to current time on server
  "pkey": "deviceId"           // partition key (optional), defaults to none (random partition)
}
 */
public class PublishRequest extends WebsocketRequest {

    private static final String TYPE = "publish";

    private String stream;
    private String sessionToken;
    private Object payload;
    private Date timestamp;
    private String pkey;

    public PublishRequest(String stream, Object payload, Date timestamp, String partitionKey, String sessionToken) {
        super(TYPE);
        this.stream = stream;
        this.sessionToken = sessionToken;
        this.payload = payload;
        this.timestamp = timestamp;
        this.pkey = partitionKey;
    }

    public String getStreamId() {
        return stream;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Object getPayload() {
        return payload;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getPartitionKey() {
        return pkey;
    }
}
