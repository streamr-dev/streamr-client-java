package com.streamr.client.rest;

public class Permission {
    private String id;
    private Boolean anonymous;
    private Operation operation;
    private String user;

    public enum Operation {
        stream_get, stream_subscribe, stream_publish, stream_delete, stream_share
    }

    public Permission(Operation operation, String user) {
        this.operation = operation;
        this.user = user;
        this.anonymous = false;
    }

    /**
     * Public permission
     */
    public Permission(Operation operation) {
        this.operation = operation;
        this.anonymous = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
