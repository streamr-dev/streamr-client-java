package com.streamr.client.utils;

import com.streamr.client.exceptions.AlreadySubscribedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.subs.Subscription;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

public class Subscriptions {

    private final Map<String, Map<Integer, Subscription>> subsByStreamIdAndPartition = new HashMap<>();

    public void add(Subscription sub) {
        Map<Integer, Subscription> subsByStreamId = subsByStreamIdAndPartition.get(sub.getStreamId());
        if (subsByStreamId == null) {
            subsByStreamId = new HashMap<>();
            subsByStreamIdAndPartition.put(sub.getStreamId(), subsByStreamId);
        }
        if (subsByStreamId.containsKey(sub.getPartition())) {
            throw new AlreadySubscribedException(sub);
        } else {
            subsByStreamId.put(sub.getPartition(), sub);
        }
    }

    public Subscription get(String streamId, int partition) throws SubscriptionNotFoundException {
        Map<Integer, Subscription> subsByStreamId = subsByStreamIdAndPartition.get(streamId);
        if (subsByStreamId == null) {
            throw new SubscriptionNotFoundException(streamId, partition);
        }
        Subscription result = subsByStreamId.get(partition);
        if (result == null) {
            throw new SubscriptionNotFoundException(streamId, partition);
        }
        return result;
    }

    public Collection<Subscription> getAllForStreamId(String streamId) {
        Map<Integer, Subscription> subsByStreamId = subsByStreamIdAndPartition.get(streamId);
        return subsByStreamId == null ? new HashSet<>() : subsByStreamId.values();
    }

    public void remove(Subscription sub) throws SubscriptionNotFoundException {
        Map<Integer, Subscription> subsByStreamId = subsByStreamIdAndPartition.get(sub.getStreamId());
        if (subsByStreamId == null) {
            throw new SubscriptionNotFoundException(sub.getStreamId(), sub.getPartition());
        }
        if (subsByStreamId.containsKey(sub.getPartition())) {
            subsByStreamId.remove(sub.getPartition());
        } else {
            throw new SubscriptionNotFoundException(sub.getStreamId(), sub.getPartition());
        }
    }

    public void forEach(Consumer<Subscription> f) {
        subsByStreamIdAndPartition.values().forEach(map -> map.values().forEach(f));
    }
}
