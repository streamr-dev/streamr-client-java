package com.streamr.client.protocol.control_layer;

public class ErrorResponse extends ControlMessage {
    public static final int TYPE = 7;
    private final String errorMessage;
    private final String errorCode;

    public ErrorResponse(String requestId, String errorMessage, String errorCode) {
        super(TYPE, requestId);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("ErrorResponse{requestId=%s, errorCode=%s, errorMessage=%s",
                getRequestId(), errorCode, errorMessage);
    }
}
