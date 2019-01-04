package com.streamr.client.rest;

import java.util.ArrayList;
import java.util.List;

/*
   "config":{
      "topic":"7wa7APtlTq6EC5iTCBy6dw",
      "fields":[
         {
            "name":"veh",
            "type":"string"
         },
         {
            "name":"spd",
            "type":"number"
         },
         {
            "name":"hdg",
            "type":"number"
         },
         {
            "name":"lat",
            "type":"number"
         },
         {
            "name":"long",
            "type":"number"
         },
         {
            "name":"line",
            "type":"string"
         }
      ]
   }
 */
public class StreamConfig {
    private List<FieldConfig> fields;

    public StreamConfig() {
        this.fields = new ArrayList<>();
    }

    public StreamConfig(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public StreamConfig addField(FieldConfig field) {
        this.fields.add(field);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        // Configs are equal if all the fields are equal (in order)
        return obj instanceof StreamConfig && ((StreamConfig) obj).getFields().equals(fields);
    }
}
