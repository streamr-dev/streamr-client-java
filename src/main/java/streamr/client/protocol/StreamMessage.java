package streamr.client.protocol;

import java.util.Date;
import java.util.Map;

public class StreamMessage {
    private String streamId;
    private int partition;
    private long timestamp;
    private Integer ttl;
    private Long offset;
    private Long previousOffset;
    private int contentTypeCode;

    // Payload type might need to be changed to Object when new
    // non-JSON payload types are introduced
    private Map<String, Object> payload;

    public StreamMessage(String streamId, int partition, long timestamp, Integer ttl, Long offset, Long previousOffset, int contentTypeCode, Map<String, Object> payload) {
        this.streamId = streamId;
        this.partition = partition;
        this.timestamp = timestamp;
        this.ttl = ttl;
        this.offset = offset;
        this.previousOffset = previousOffset;
        this.contentTypeCode = contentTypeCode;
        this.payload = payload;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getPartition() {
        return partition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }

    public Integer getTtl() {
        return ttl;
    }

    public Long getOffset() {
        return offset;
    }

    public Long getPreviousOffset() {
        return previousOffset;
    }

    public int getContentTypeCode() {
        return contentTypeCode;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
