package com.streamr.client;

import com.squareup.moshi.Moshi;
import com.streamr.client.utils.StringOrMillisDateJsonAdapter;

import java.util.Date;

/**
 * Provides the barebones of a StreamrClient, including
 * holding the config, providing JSON serializers etc.
 */
public abstract class AbstractStreamrClient {

    // Thread safe
    protected static final Moshi MOSHI = new Moshi.Builder()
            .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
            .build();

    protected final StreamrClientOptions options;

    public AbstractStreamrClient(StreamrClientOptions config) {
        this.options = config;
    }

}
