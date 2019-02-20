package com.streamr.client;

import com.squareup.moshi.Moshi;
import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.authentication.ChallengeAuthenticationMethod;
import com.streamr.client.authentication.Session;
import com.streamr.client.utils.HttpUtils;

/**
 * Provides the barebones of a StreamrClient, including
 * holding the config, providing JSON serializers etc.
 */
public abstract class AbstractStreamrClient {

    // Thread safe
    protected static final Moshi MOSHI = HttpUtils.MOSHI;

    protected final StreamrClientOptions options;

    protected final Session session;

    public AbstractStreamrClient(StreamrClientOptions options) {
        this.options = options;

        // Create Session object based on what kind of authentication method is provided in options
        if (options.getApiKey() != null) {
            session = new Session(new ApiKeyAuthenticationMethod(options));
        } else if (options.getAccount() != null) {
            session = new Session(new ChallengeAuthenticationMethod(options));
        } else {
            session = new Session();
        }
    }

}
