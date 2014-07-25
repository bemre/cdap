/*
 * Copyright 2014 Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.continuuity.explore.client;

import com.continuuity.proto.QueryResult;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Results of an Explore statement execution.
 */
public interface ExploreExecutionResult extends Iterator<QueryResult>, Closeable {

  /**
   * @return the current fetch size for this object
   */
  int getFetchSize();

  /**
   * Gives this object a hint as to the number of rows that should be fetched from the server when more rows are needed
   * for this object. If the fetch size specified is zero, this object ignores the value and is free to make its own
   * best guess as to what the fetch size should be. The fetch size may be changed at any time.
   *
   * @param fetchSize the number of rows to fetch
   */
  void setFetchSize(int fetchSize);
}
