package com.streamr.client.protocol.rest;

import com.streamr.client.protocol.java.util.Objects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code StreamConfig} holds a configuration of {@code FieldConfig } objects.
 *
 * <p>Below is an example of Stream configuration in JSON format.
 *
 * <pre>
 * +config":{
 *   "topic":"7wa7APtlTq6EC5iTCBy6dw",
 *   "fields":[
 *     {
 *       "name":"veh",
 *       "type":"string"
 *     },
 *     {
 *       "name":"spd",
 *       "type":"number"
 *     },
 *     {
 *       "name":"hdg",
 *       "type":"number"
 *     },
 *     {
 *       "name":"lat",
 *       "type":"number"
 *     },
 *     {
 *       "name":"long",
 *       "type":"number"
 *     },
 *     {
 *       "name":"line",
 *       "type":"string"
 *     }
 *   ]
 * }
 * </pre>
 */
public final class StreamConfig {
  private final List<FieldConfig> fields;

  public StreamConfig() {
    this.fields = new ArrayList<>();
  }

  public StreamConfig(final FieldConfig... fields) {
    Objects.requireNonNull(fields);
    this.fields = Arrays.asList(fields);
  }

  public List<FieldConfig> getFields() {
    return fields;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final StreamConfig that = (StreamConfig) obj;
    return Objects.equals(fields, that.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields);
  }
}
