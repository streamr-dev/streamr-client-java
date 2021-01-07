package com.streamr.client.rest;

import java.util.Objects;

public final class Permission {
    private final String id;
    private final Boolean anonymous;
    private final Operation operation;
    private final String user;

    public enum Operation {
        stream_get, stream_subscribe, stream_publish, stream_delete, stream_share
    }

    public Permission(final String id, final Boolean anonymous, final Operation operation, final String user) {
        this.id = id;
        this.anonymous = anonymous;
        this.operation = operation;
        this.user = user;
    }

    public Permission(final Operation operation, final String user) {
        this.operation = operation;
        this.user = user;
        this.anonymous = false;
        this.id = null;
    }

    /**
     * Public permission
     */
    public Permission(final Operation operation) {
        this.operation = operation;
        this.anonymous = true;
        this.user = null;
        this.id = null;
    }

    public String getId() {
        return id;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getUser() {
        return user;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Permission that = (Permission) obj;
        return Objects.equals(id, that.id) && Objects.equals(anonymous, that.anonymous) && operation == that.operation && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, anonymous, operation, user);
    }

    @Override
    public String toString() {
        return String.format("Permission{id='%s', anonymous=%s, operation=%s, user='%s'}", id, anonymous, operation, user);
    }
}
