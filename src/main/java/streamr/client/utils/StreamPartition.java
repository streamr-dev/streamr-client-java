package streamr.client.utils;

public class StreamPartition {

    private String streamId;
    private int partition;

    public StreamPartition(String streamId, int partition) {
        this.streamId = streamId;
        this.partition = partition;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getPartition() {
        return partition;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StreamPartition
                && ((StreamPartition) obj).getStreamId().equals(streamId)
                && ((StreamPartition) obj).getPartition() == partition;
    }

    @Override
    public int hashCode() {
        return (this.getStreamId() + "-" + getPartition()).hashCode();
    }

    @Override
    public String toString() {
        return "streamId: " + this.getStreamId() + ", partition: " + getPartition();
    }
}
