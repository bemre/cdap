/*
 * Copyright 2014 Continuuity,Inc. All Rights Reserved.
 */
package com.continuuity.data.file;

import com.continuuity.data.file.filter.AndReadFilter;

/**
 * Utility functions for {@link com.continuuity.data.file.ReadFilter}.
 */
public class ReadFilters {
  public static final ReadFilter and(ReadFilter lhs, ReadFilter rhs) {
    return new AndReadFilter(lhs, rhs);
  }
}