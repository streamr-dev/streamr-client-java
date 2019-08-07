package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public abstract class KeyStorage {
    public KeyStorage(HashMap<String, GroupKey> publisherGroupKeys){

    }

    public abstract boolean hasKey(String streamId);

    public abstract GroupKey getLatestKey(String streamId);

    public abstract ArrayList<GroupKey> getKeysBetween(String streamId, Date start, Date end);

    public abstract void addKey(String streamId, GroupKey key);
}
