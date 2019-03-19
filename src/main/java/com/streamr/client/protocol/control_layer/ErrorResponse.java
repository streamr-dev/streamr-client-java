package com.streamr.client.protocol.control_layer;

public class ErrorResponse extends ControlMessage {
    public static final int TYPE = 7;
    private final String errorMessage;

    public ErrorResponse(String errorMessage) {
        super(TYPE);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
