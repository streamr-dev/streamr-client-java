package com.streamr.client.rest;

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
    private String topic;
    private List<FieldConfig> fields;

    public String getTopic() {
        return topic;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }
}
