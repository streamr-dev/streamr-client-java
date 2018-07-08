package com.streamr.client.rest;

import com.squareup.moshi.Json;

/*
         {
            "name":"veh",
            "type":"string"
         }
 */
public class FieldConfig {

    enum Type {
        @Json(name = "number") NUMBER,
        @Json(name = "string") STRING,
        @Json(name = "boolean") BOOLEAN,
        @Json(name = "list") LIST,
        @Json(name = "map") MAP
    }

    private String name;
    private Type type;
}
