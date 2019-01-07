package streamr.client.exceptions;

public class PartitionNotSpecifiedException extends RuntimeException {
    public PartitionNotSpecifiedException(String streamId, int partitionCount) {
        super("Stream " + streamId + " has " + partitionCount + " partitions. You must specify which one you want to subscribe to!");
    }
}
