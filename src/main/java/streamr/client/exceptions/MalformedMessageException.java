package streamr.client.exceptions;

public class MalformedMessageException extends RuntimeException {

    public MalformedMessageException(String message) {
        super(message);
    }

    public MalformedMessageException(String message, Throwable cause) {
        super(message, cause);
    }

}
