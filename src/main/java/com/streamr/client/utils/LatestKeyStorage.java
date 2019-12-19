package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/*
This key storage is used when the publisher only wants to store the latest key used to encrypt messages,
not the previous ones. This will prevent the publisher to answer historical group key requests.
 */
public class LatestKeyStorage extends KeyStorage {
    private final HashMap<String, GroupKey> latestGroupKeys;

    public LatestKeyStorage(HashMap<String, GroupKey> publisherGroupKeys) {
        super();
        latestGroupKeys = publisherGroupKeys == null ? new HashMap<>() : publisherGroupKeys;
    }

    public LatestKeyStorage() {
        this(null);
    }

    @Override
    public boolean hasKey(String streamId) {
        return latestGroupKeys.containsKey(streamId);
    }

    @Override
    public GroupKey getLatestKey(String streamId) {
        return latestGroupKeys.get(streamId);
    }

    @Override
    public ArrayList<GroupKey> getKeysBetween(String streamId, Date start, Date end) {
        throw new InvalidGroupKeyRequestException("Cannot retrieve historical keys for stream " + streamId
                + " between " + start + " and " + end + " because only the latest key is stored.");
    }

    @Override
    public void addKey(String streamId, GroupKey key) {
        latestGroupKeys.put(streamId, key);
    }
}
