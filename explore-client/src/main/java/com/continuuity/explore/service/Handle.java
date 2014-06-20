package com.continuuity.explore.service;

import com.google.common.base.Objects;

import java.util.UUID;

/**
 * Represents an operation that is submitted for execution to {@link Explore}.
 */
public class Handle {
  private final String handle;

  public static Handle generate() {
    // TODO: make sure handles are unique across multiple instances. - REACTOR-272
    return new Handle(UUID.randomUUID().toString());
  }

  public static Handle fromId(String id) {
    return new Handle(id);
  }

  private Handle(String handle) {
    this.handle = handle;
  }

  public String getHandle() {
    return handle;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", handle)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Handle that = (Handle) o;

    return Objects.equal(this.handle, that.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(handle);
  }
}