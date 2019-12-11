package com.streamr.client.utils;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
/*
This util contains methods to check if an Ethereum address is a valid publisher/subscriber (using REST endpoints).
 */
public class AddressValidityUtil {
    private final Cache<String, HashMap<String, Boolean>> subscribersPerStreamId = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
            .expireAfterWrite(30, TimeUnit.MINUTES).build();
    private final Function<String, List<String>> getSubscribersFunction;
    private final BiFunction<String, String, Boolean> isSubscriberFunction;
    private final Cache<String, HashMap<String, Boolean>> publishersPerStreamId = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
            .expireAfterWrite(30, TimeUnit.MINUTES).build();
    private final Function<String, List<String>> getPublishersFunction;
    private final BiFunction<String, String, Boolean> isPublisherFunction;

    public AddressValidityUtil(Function<String, List<String>> getSubscribersFunction, BiFunction<String, String, Boolean> isSubscriberFunction,
                               Function<String, List<String>> getPublishersFunction, BiFunction<String, String, Boolean> isPublisherFunction) {
        this.getSubscribersFunction = getSubscribersFunction;
        this.isSubscriberFunction = isSubscriberFunction;
        this.getPublishersFunction = getPublishersFunction;
        this.isPublisherFunction = isPublisherFunction;
    }

    public boolean isValidSubscriber(String streamId, String subscriberId) {
        return isValid(streamId, subscriberId, this::getSubscribers, isSubscriberFunction);
    }

    public boolean isValidPublisher(String streamId, String publisherId) {
        return isValid(streamId, publisherId, this::getPublishers, isPublisherFunction);
    }

    public void clearAndClose() {
        subscribersPerStreamId.clearAndClose();
        publishersPerStreamId.clearAndClose();
    }

    private boolean isValid(String streamId, String address, Function<String, HashMap<String, Boolean>> getAddresses,
                            BiFunction<String, String, Boolean> isFunction) {
        // check the local cache
        Boolean valid = getAddresses.apply(streamId).get(address);
        if (valid == null) { // cache miss
            valid = isFunction.apply(streamId, address);
            // update cache
            getAddresses.apply(streamId).put(address, valid);
        }
        return valid;
    }

    private HashMap<String, Boolean> getSubscribers(String streamId) {
        return getAddresses(streamId, subscribersPerStreamId, getSubscribersFunction);
    }

    private HashMap<String, Boolean> getPublishers(String streamId) {
        return getAddresses(streamId, publishersPerStreamId, getPublishersFunction);
    }

    private HashMap<String, Boolean> getAddresses(String streamId, Cache<String, HashMap<String, Boolean>> cache,
                                                  Function<String, List<String>> getFunction) {
        HashMap<String, Boolean> addresses = safeGetCache(cache).get(streamId);
        if (addresses == null) {
            addresses = new HashMap<>();
            for (String publisher: getFunction.apply(streamId)) {
                addresses.put(publisher, true);
            }
            safeGetCache(cache).put(streamId, addresses);
        }
        return addresses;
    }

    private Cache<String, HashMap<String, Boolean>> safeGetCache(Cache<String, HashMap<String, Boolean>> cache) {
        if (cache.isClosed()) {
            cache = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
                    .expireAfterWrite(30, TimeUnit.MINUTES).build();
        }
        return cache;
    }
}
