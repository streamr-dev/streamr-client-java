package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidResendResponseException;
import com.streamr.client.protocol.control_layer.ResendResponse;
import com.streamr.client.protocol.control_layer.ResendResponseNoResend;
import com.streamr.client.protocol.control_layer.ResendResponseResent;
import com.streamr.client.subs.Subscription;

import java.util.HashMap;

public class ResendUtil {
    private long counter = 0L;
    private HashMap<String, Subscription> subForRequestId = new HashMap<>();

    public String generateRequestId() {
        String requestId = "" + counter;
        counter++;
        return requestId;
    }

    public Subscription getSub(ResendResponse resendResponse) throws InvalidResendResponseException {
        if (!subForRequestId.containsKey(resendResponse.getRequestId())) {
            throw new InvalidResendResponseException("No subscription for requestId in this response: " + resendResponse.toJson());
        }
        return subForRequestId.get(resendResponse.getRequestId());
    }

    public void deleteDoneSub(ResendResponse resendResponse) {
        if (resendResponse.getType() == ResendResponseResent.TYPE || resendResponse.getType() == ResendResponseNoResend.TYPE) {
            subForRequestId.remove(resendResponse.getRequestId());
        }
    }

    public String registerResendForSub(Subscription sub) {
        String requestId = generateRequestId();
        subForRequestId.put(requestId, sub);
        // sub.addpending...
        return requestId;
    }
}
