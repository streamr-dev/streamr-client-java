package com.streamr.client;

import com.streamr.client.protocol.control_layer.ErrorResponse;

@FunctionalInterface
interface ErrorMessageHandler {
    void onErrorMessage(ErrorResponse error);
}
