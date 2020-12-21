package com.streamr.client.utils;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/*
This util contains methods to check if an Ethereum address is a valid publisher/subscriber (using REST endpoints).
It also contains methods to get the number of subscribers to revoke for a stream.
 */
public class AddressValidityUtil {
    private final Map<String, HashSet<Address>> localSubscribersSets = new HashMap<>();
    private final Cache<String, Map<Address, Boolean>> subscribersPerStreamId = CacheFactory.build();
    private final Function<String, List<Address>> getSubscribersFunction;
    private final BiFunction<String, Address, Boolean> isSubscriberFunction;
    private final Cache<String, Map<Address, Boolean>> publishersPerStreamId = CacheFactory.build();
    private final Function<String, List<Address>> getPublishersFunction;
    private final BiFunction<String, Address, Boolean> isPublisherFunction;

    /**
     *
     * @param getSubscribersFunction (streamId)
     * @param isSubscriberFunction (streamId, address)
     * @param getPublishersFunction (streamId)
     * @param isPublisherFunction (streamId, address)
     */
    public AddressValidityUtil(Function<String, List<Address>> getSubscribersFunction, BiFunction<String, Address, Boolean> isSubscriberFunction,
                               Function<String, List<Address>> getPublishersFunction, BiFunction<String, Address, Boolean> isPublisherFunction) {
        this.getSubscribersFunction = getSubscribersFunction;
        this.isSubscriberFunction = isSubscriberFunction;
        this.getPublishersFunction = getPublishersFunction;
        this.isPublisherFunction = isPublisherFunction;
    }

    public int nbSubscribersToRevoke(String streamId) {
        HashSet<Address> realSubscribersSet = new HashSet<>(getSubscribersFunction.apply(streamId));
        int counter = 0;
        for (Address subscriberId: localSubscribersSets.getOrDefault(streamId, new HashSet<>())) {
            if (!realSubscribersSet.contains(subscriberId)) {
                counter++;
            }
        }
        localSubscribersSets.put(streamId, realSubscribersSet);
        return counter;
    }

    public Set<Address> getSubscribersSet(String streamId, boolean locally) {
        return locally ? localSubscribersSets.get(streamId) : new HashSet<>(getSubscribersFunction.apply(streamId));
    }

    public boolean isValidSubscriber(String streamId, Address subscriberId) {
        return isValid(streamId, subscriberId, this::getSubscribers, isSubscriberFunction);
    }

    public boolean isValidPublisher(String streamId, Address publisherId) {
        return isValid(streamId, publisherId, this::getPublishers, isPublisherFunction);
    }

    public void clearAndClose() {
        subscribersPerStreamId.clearAndClose();
        publishersPerStreamId.clearAndClose();
    }

    private static boolean isValid(String streamId, Address address, Function<String, Map<Address, Boolean>> getAddresses,
                            BiFunction<String, Address, Boolean> isFunction) {
        // check the local cache
        Boolean valid = getAddresses.apply(streamId).get(address);
        if (valid == null) { // cache miss
            valid = isFunction.apply(streamId, address);
            // update cache
            getAddresses.apply(streamId).put(address, valid);
        }
        return valid;
    }

    private Map<Address, Boolean> getSubscribers(String streamId) {
        return getAddresses(streamId, subscribersPerStreamId, getSubscribersFunction);
    }

    private Map<Address, Boolean> getPublishers(String streamId) {
        return getAddresses(streamId, publishersPerStreamId, getPublishersFunction);
    }

    private Map<Address, Boolean> getAddresses(String streamId, Cache<String, Map<Address, Boolean>> cache,
                                                  Function<String, List<Address>> getFunction) {
        Map<Address, Boolean> addresses = safeGetCache(cache).get(streamId);
        if (addresses == null) {
            addresses = new HashMap<>();
            for (Address address: getFunction.apply(streamId)) {
                addresses.put(address, true);
            }
            safeGetCache(cache).put(streamId, addresses);
        }
        return addresses;
    }

    private static Cache<String, Map<Address, Boolean>> safeGetCache(Cache<String, Map<Address, Boolean>> cache) {
        if (cache.isClosed()) {
            cache = CacheFactory.build();
        }
        return cache;
    }

    private static class CacheFactory {
        private static final long CACHE_EXPIRATION = 30L; // in minutes

        static Cache<String, Map<Address, Boolean>> build() {
            final Cache<String, Map<Address, Boolean>> cache = new Cache2kBuilder<String, Map<Address, Boolean>>() {}
                    .expireAfterWrite(CACHE_EXPIRATION, TimeUnit.MINUTES)
                    .build();
            return cache;
        }
    }
}
