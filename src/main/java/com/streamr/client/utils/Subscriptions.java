package com.streamr.client.utils;

import com.streamr.client.Subscription;
import com.streamr.client.exceptions.AlreadySubscribedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;

import java.util.*;

public class Subscriptions {

    private final Map<StreamPartition, Subscription> subs = new HashMap<>();
    private final Map<String, Subscription> subsById = new HashMap<>();

    public void add(Subscription sub) {
        if (subs.containsKey(sub.getStreamPartition())) {
            throw new AlreadySubscribedException(sub);
        } else {
            subs.put(sub.getStreamPartition(), sub);
            subsById.put(sub.getId(), sub);
        }
    }

    public Subscription get(Subscription sub) throws SubscriptionNotFoundException {
        Subscription result = subs.get(sub.getStreamPartition());
        if (result == null) {
            throw new SubscriptionNotFoundException(sub);
        }
        return result;
    }

    public Subscription get(String subId) throws SubscriptionNotFoundException {
        Subscription result = subsById.get(subId);
        if (result == null) {
            throw new SubscriptionNotFoundException(subId);
        }
        return result;
    }

    public void remove(Subscription sub) throws SubscriptionNotFoundException {
        if (subs.containsKey(sub.getStreamPartition())) {
            Subscription removed = subs.remove(sub.getStreamPartition());
            subsById.remove(removed.getId());
        } else {
            throw new SubscriptionNotFoundException(sub);
        }
    }
}
