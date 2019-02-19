package com.streamr.client.rest;

import java.util.Date;

/*
{
   "id":"7wa7APtlTq6EC5iTCBy6dw",
   "partitions":1,
   "name":"Public transport demo",
   "feed":{
      "id":7,
      "name":"API",
      "module":147
   },
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
   },
   "description":"Helsinki tram data by HSL",
   "uiChannel":false,
   "dateCreated":"2016-05-27T15:46:30Z",
   "lastUpdated":"2017-04-10T16:04:38Z"
}
 */
public class Stream {
    private String id;
    private String name;
    private String description;
    private Integer partitions;
    private StreamConfig config;
    private Boolean uiChannel;
    private Date dateCreated;
    private Date lastUpdated;

    public Stream(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public StreamConfig getConfig() {
        return config;
    }

    public void setConfig(StreamConfig config) {
        this.config = config;
    }

    public Boolean isUiChannel() {
        return uiChannel;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
}
