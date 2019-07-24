package com.streamr.client.rest;

public class UserInfo {
    private final String name;
    private final String username;
    // in the edge case where the user is an anonymous API key, this id is the key (username is null).
    private final String id;

    public UserInfo(String name, String username, String id) {
        this.name = name;
        this.username = username;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }
}
