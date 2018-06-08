package com.streamr.client.utils;

import com.streamr.client.Subscription;
import com.streamr.client.exceptions.AlreadySubscribedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class Subscriptions {

    private final Map<String, Subscription> subsByKey = new HashMap<>();

    private static String getKey(String streamId, int partition) {
        return streamId + "-" + partition;
    }

    public void add(Subscription sub) {
        String key = getKey(sub.getStreamId(), sub.getPartition());
        if (subsByKey.containsKey(key)) {
            throw new AlreadySubscribedException(sub);
        } else {
            subsByKey.put(key, sub);
        }
    }

    public Subscription get(String streamId, int partition) throws SubscriptionNotFoundException {
        String key = getKey(streamId, partition);

        Subscription result = subsByKey.get(key);
        if (result == null) {
            throw new SubscriptionNotFoundException(streamId, partition);
        }
        return result;
    }

    public void remove(Subscription sub) throws SubscriptionNotFoundException {
        String key = getKey(sub.getStreamId(), sub.getPartition());
        if (subsByKey.containsKey(key)) {
            subsByKey.remove(key);
        } else {
            throw new SubscriptionNotFoundException(sub.getStreamId(), sub.getPartition());
        }
    }
}
