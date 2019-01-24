package com.streamr.client.authentication;

/**
 * Holds credentials for getting new sessionTokens, and holds the current sessionToken.
 * Currently only supports the API key. Support for Ethereum-based authentication needs to
 * be added later.
 */
public class Session {

    private final AuthenticationMethod authenticationMethod;
    private String sessionToken = null;

    /**
     * For unauthenticated sessions
     */
    public Session() {
        authenticationMethod = null;
    }

    public Session(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public boolean isAuthenticated() {
        return authenticationMethod != null;
    }

    public String getSessionToken() {
        if (sessionToken == null && isAuthenticated()) {
            sessionToken = authenticationMethod.newSessionToken();
        } else if (sessionToken == null) {
            throw new RuntimeException("No authentication method: Don't call getSessionToken() if isAuthenticated returns false!");
        }

        return sessionToken;
    }

}
