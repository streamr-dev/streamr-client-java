package com.streamr.client.rest;

import java.util.Objects;

public final class UserInfo {
    private final String name;
    private final String username;

    public UserInfo(final String name, final String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final UserInfo userInfo = (UserInfo) obj;
        return Objects.equals(name, userInfo.name) && Objects.equals(username, userInfo.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, username);
    }

    @Override
    public String toString() {
        return String.format("UserInfo{name='%s', username='%s'}", name, username);
    }
}
