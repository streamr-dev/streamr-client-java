package streamr.client.protocol;

public class UnsubscribeRequest extends WebsocketRequest {

    private static final String TYPE = "unsubscribe";

    private String stream;
    private Integer partition = 0;

    public UnsubscribeRequest(String stream) {
        super(TYPE);
        this.stream = stream;
    }

    public UnsubscribeRequest(String stream, Integer partition) {
        super(TYPE);
        this.stream = stream;
        this.partition = partition;
    }

    public String getStream() {
        return stream;
    }

    public Integer getPartition() {
        return partition;
    }
}
