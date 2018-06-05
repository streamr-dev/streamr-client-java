package com.streamr.client.exceptions;

import com.streamr.client.Subscription;

public class SubscriptionNotFoundException extends Exception {

    public SubscriptionNotFoundException(Subscription sub) {
        super("Subscription not found: " + sub);
    }

    public SubscriptionNotFoundException(String subId) {
        super("Subscription not found by id: " + subId);
    }
}
