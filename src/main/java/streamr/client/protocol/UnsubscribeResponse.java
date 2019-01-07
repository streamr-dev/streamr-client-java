package streamr.client.protocol;

public class UnsubscribeResponse {

    private String stream;
    private int partition;

    public String getStream() {
        return stream;
    }

    public int getPartition() {
        return partition;
    }
}
