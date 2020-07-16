package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;

import java.util.ArrayList;

public interface KeyStorage {

    boolean hasKey(String streamId);

    /**
    Returns null when no key is stored.
     */
    GroupKey getLatestKey(String streamId);

    ArrayList<GroupKey> getKeysBetween(String streamId, long start, long end) throws InvalidGroupKeyRequestException;

    void addKey(String streamId, GroupKey key);
}
