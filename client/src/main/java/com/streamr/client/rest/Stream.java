package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;
import com.streamr.client.utils.Address;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@code Stream} represents a Streamr Stream.
 *
 * <p>Below is an example of Stream in JSON format.
 *
 * <pre>
 * {
 *    "id":"7wa7APtlTq6EC5iTCBy6dw",
 *    "partitions":1,
 *    "name":"Public transport demo",
 *    "feed":{
 *       "id":7,
 *       "name":"API",
 *       "module":147
 *    },
 *    "config":{
 *       "topic":"7wa7APtlTq6EC5iTCBy6dw",
 *       "fields":[
 *          {
 *             "name":"veh",
 *             "type":"string"
 *          },
 *          {
 *             "name":"spd",
 *             "type":"number"
 *          },
 *          {
 *             "name":"hdg",
 *             "type":"number"
 *          },
 *          {
 *             "name":"lat",
 *             "type":"number"
 *          },
 *          {
 *             "name":"long",
 *             "type":"number"
 *          },
 *          {
 *             "name":"line",
 *             "type":"string"
 *          }
 *       ]
 *    },
 *    "description":"Helsinki tram data by HSL",
 *    "uiChannel":false,
 *    "dateCreated":"2016-05-27T15:46:30Z",
 *    "lastUpdated":"2017-04-10T16:04:38Z"
 * }
 * </pre>
 */
public final class Stream {
  private final String id;
  private final String name;
  private final String description;
  private final int partitions;
  private final StreamConfig config;
  private final Boolean uiChannel;
  private final boolean requireSignedData;
  private final boolean requireEncryptedData;
  private final Date dateCreated;
  private final Date lastUpdated;

  private Stream(
      final String id,
      final String name,
      final String description,
      final int partitions,
      final StreamConfig config,
      final Boolean uiChannel,
      final boolean requireSignedData,
      final boolean requireEncryptedData,
      final Date dateCreated,
      final Date lastUpdated) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.partitions = partitions;
    this.config = config;
    this.uiChannel = uiChannel;
    this.requireSignedData = requireSignedData;
    this.requireEncryptedData = requireEncryptedData;
    this.dateCreated = dateCreated;
    this.lastUpdated = lastUpdated;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getPartitions() {
    return partitions;
  }

  public StreamConfig getConfig() {
    return config;
  }

  public Boolean isUiChannel() {
    return uiChannel;
  }

  public boolean requiresSignedData() {
    return requireSignedData;
  }

  public boolean requiresEncryptedData() {
    return requireEncryptedData;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public List<StreamPart> toStreamParts() {
    List<StreamPart> result = new ArrayList<>();
    for (int i = 0; i < this.partitions; i++) {
      result.add(new StreamPart(this.id, i));
    }
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Stream stream = (Stream) obj;
    return partitions == stream.partitions
        && requireSignedData == stream.requireSignedData
        && requireEncryptedData == stream.requireEncryptedData
        && Objects.equals(id, stream.id)
        && Objects.equals(name, stream.name)
        && Objects.equals(description, stream.description)
        && Objects.equals(config, stream.config)
        && Objects.equals(uiChannel, stream.uiChannel)
        && Objects.equals(dateCreated, stream.dateCreated)
        && Objects.equals(lastUpdated, stream.lastUpdated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        name,
        description,
        partitions,
        config,
        uiChannel,
        requireSignedData,
        requireEncryptedData,
        dateCreated,
        lastUpdated);
  }

  public static final class Builder {
    private String id;
    private String name;
    private String description;
    private int partitions = 1;
    private StreamConfig config;
    private Boolean uiChannel;
    private boolean requireSignedData;
    private boolean requireEncryptedData;
    private Date dateCreated;
    private Date lastUpdated;

    public Builder() {}

    public Builder(final Stream stream) {
      this.id = stream.id;
      this.name = stream.name;
      this.description = stream.description;
      this.partitions = stream.partitions;
      this.config = stream.config;
      this.uiChannel = stream.uiChannel;
      this.requireSignedData = stream.requireSignedData;
      this.requireEncryptedData = stream.requireEncryptedData;
      this.dateCreated = stream.dateCreated;
      this.lastUpdated = stream.lastUpdated;
    }

    public Builder withId(final String id) {
      this.id = id;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withPartitions(final int partitions) {
      this.partitions = partitions;
      return this;
    }

    public Builder withConfig(final StreamConfig config) {
      this.config = config;
      return this;
    }

    public Builder withUiChannel(final Boolean uiChannel) {
      this.uiChannel = uiChannel;
      return this;
    }

    public Builder withRequireSignedData(final boolean requireSignedData) {
      this.requireSignedData = requireSignedData;
      return this;
    }

    public Builder withRequireEncryptedData(final boolean requireEncryptedData) {
      this.requireEncryptedData = requireEncryptedData;
      return this;
    }

    public Builder withDateCreated(final Instant dateCreated) {
      if (dateCreated != null) {
        this.dateCreated = Date.from(dateCreated);
      } else {
        this.dateCreated = null;
      }
      return this;
    }

    public Builder withLastUpdated(final Instant lastUpdated) {
      if (lastUpdated != null) {
        this.lastUpdated = Date.from(lastUpdated);
      } else {
        this.lastUpdated = null;
      }
      return this;
    }

    public Stream createStream() {
      return new Stream(
          id,
          name,
          description,
          partitions,
          config,
          uiChannel,
          requireSignedData,
          requireEncryptedData,
          dateCreated,
          lastUpdated);
    }
  }

  public static String createStreamId(String streamIdOrPath, Address owner) {
    if (streamIdOrPath == null) {
        throw new IllegalArgumentException("Missing stream id");
    }
    if (!streamIdOrPath.startsWith("/")) {
        return streamIdOrPath;
    }
    if (owner == null) {
        throw new Error("Owner missing for stream id: " + streamIdOrPath);
    }
    return owner.toString().toLowerCase() + streamIdOrPath;
  }
}
