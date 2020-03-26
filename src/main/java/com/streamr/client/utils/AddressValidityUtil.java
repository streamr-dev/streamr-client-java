package com.streamr.client.utils;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
This util contains methods to check if an Ethereum address is a valid publisher/subscriber (using REST endpoints).
It also contains methods to get the number of subscribers to revoke for a stream.
 */
public class AddressValidityUtil {
    private static final int CACHE_EXPIRATION = 30; // in minutes
    private final HashMap<String, HashSet<String>> localSubscribersSets = new HashMap<>();
    private final Cache<String, HashMap<String, Boolean>> subscribersPerStreamId = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
            .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.MINUTES).build();
    private final Function<String, List<String>> getSubscribersFunction;
    private final BiFunction<String, String, Boolean> isSubscriberFunction;
    private final Cache<String, HashMap<String, Boolean>> publishersPerStreamId = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
            .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.MINUTES).build();
    private final Function<String, List<String>> getPublishersFunction;
    private final BiFunction<String, String, Boolean> isPublisherFunction;

    public AddressValidityUtil(Function<String, List<String>> getSubscribersFunction, BiFunction<String, String, Boolean> isSubscriberFunction,
                               Function<String, List<String>> getPublishersFunction, BiFunction<String, String, Boolean> isPublisherFunction) {
        this.getSubscribersFunction = getSubscribersFunction;
        this.isSubscriberFunction = isSubscriberFunction;
        this.getPublishersFunction = getPublishersFunction;
        this.isPublisherFunction = isPublisherFunction;
    }

    public int nbSubscribersToRevoke(String streamId) {
        HashSet<String> realSubscribersSet = new HashSet<>(getSubscribersFunction.apply(streamId));
        int counter = 0;
        for (String subscriberId: localSubscribersSets.getOrDefault(streamId, new HashSet<>())) {
            if (!realSubscribersSet.contains(subscriberId)) {
                counter++;
            }
        }
        localSubscribersSets.put(streamId, realSubscribersSet);
        return counter;
    }

    public HashSet<String> getSubscribersSet(String streamId, boolean locally) {
        return locally ? localSubscribersSets.get(streamId) : new HashSet<>(getSubscribersFunction.apply(streamId));
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

    private static boolean isValid(String streamId, String address, Function<String, HashMap<String, Boolean>> getAddresses,
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

    private static Cache<String, HashMap<String, Boolean>> safeGetCache(Cache<String, HashMap<String, Boolean>> cache) {
        if (cache.isClosed()) {
            cache = new Cache2kBuilder<String, HashMap<String, Boolean>>() {}
                    .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.MINUTES).build();
        }
        return cache;
    }
}
