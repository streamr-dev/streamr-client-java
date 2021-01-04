package com.streamr.client.rest;

public class UserInfo {
    private final String name;
    private final String username;

    public UserInfo(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }
}
