package com.streamr.client.utils;

import java.util.HashMap;

public class KeyStorageUtil {
    public static KeyStorage getKeyStorage(HashMap<String, GroupKey> publisherGroupKeys, boolean storeHistoricalKeys) {
        if (storeHistoricalKeys) {
            return new KeyHistoryStorage(publisherGroupKeys);
        }
        return new LatestKeyStorage(publisherGroupKeys);
    }
}
