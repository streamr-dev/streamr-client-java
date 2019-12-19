package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public interface KeyStorage {

    boolean hasKey(String streamId);

    /**
    Returns null when no key is stored.
     */
    UnencryptedGroupKey getLatestKey(String streamId);

    ArrayList<UnencryptedGroupKey> getKeysBetween(String streamId, Date start, Date end);

    void addKey(String streamId, UnencryptedGroupKey key);
}
