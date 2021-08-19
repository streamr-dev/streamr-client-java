package com.streamr.client.protocol.rest;

import com.squareup.moshi.Json;
import com.streamr.client.protocol.java.util.Objects;

/**
 * {@code FieldConfig} holds Stream's field configuration.
 *
 * <p>Below is an example of Field configuration in JSON format.
 *
 * <pre>
 * {
 *   "name":"veh",
 *   "type":"string"
 * }
 * </pre>
 */
public final class FieldConfig {
  public enum Type {
    @Json(name = "number")
    NUMBER,
    @Json(name = "string")
    STRING,
    @Json(name = "boolean")
    BOOLEAN,
    @Json(name = "list")
    LIST,
    @Json(name = "map")
    MAP
  }

  private final String name;
  private final Type type;

  public FieldConfig(final String name, final Type type) {
    Objects.requireNonNull(name);
    this.name = name;
    Objects.requireNonNull(type);
    this.type = type;
  }

  public static FieldConfig createNumber(final String name) {
    return new FieldConfig(name, Type.NUMBER);
  }

  public static FieldConfig createString(final String name) {
    return new FieldConfig(name, Type.STRING);
  }

  public static FieldConfig createBoolean(final String name) {
    return new FieldConfig(name, Type.BOOLEAN);
  }

  public static FieldConfig createList(final String name) {
    return new FieldConfig(name, Type.LIST);
  }

  public static FieldConfig createMap(final String name) {
    return new FieldConfig(name, Type.MAP);
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final FieldConfig that = (FieldConfig) obj;
    return Objects.equals(name, that.name) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }
}
