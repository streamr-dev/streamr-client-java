package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public interface KeyStorage {

    boolean hasKey(String streamId);

    /**
    Returns null when no key is stored.
     */
    UnencryptedGroupKey getLatestKey(String streamId);

    ArrayList<UnencryptedGroupKey> getKeysBetween(String streamId, long start, long end) throws InvalidGroupKeyRequestException;

    void addKey(String streamId, UnencryptedGroupKey key);
}
