package com.streamr.client.rest;

import com.squareup.moshi.Json;

/*
         {
            "name":"veh",
            "type":"string"
         }
 */
public class FieldConfig {

    public enum Type {
        @Json(name = "number") NUMBER,
        @Json(name = "string") STRING,
        @Json(name = "boolean") BOOLEAN,
        @Json(name = "list") LIST,
        @Json(name = "map") MAP
    }

    private String name;
    private Type type;

    public FieldConfig(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FieldConfig
                && ((FieldConfig) obj).getName().equals(name)
                && ((FieldConfig) obj).getType().equals(type);
    }
}
