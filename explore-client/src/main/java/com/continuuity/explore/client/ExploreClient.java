/*
 * Copyright 2012-2014 Continuuity, Inc.
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

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;

/**
 * Explore client discovers explore service, and executes explore commands using the service.
 */
public interface ExploreClient extends Closeable {

  /**
   * Returns true if the explore service is up and running.
   */
  boolean isServiceAvailable();

  /**
   * Enables ad-hoc exploration of the given {@link com.continuuity.api.data.batch.RecordScannable}.
   *
   * @param datasetInstance dataset instance name.
   * @return a {@code Future} object that can either successfully complete, or enter a failed state depending on
   *         the success of the enable operation.
   */
  ListenableFuture<Void> enableExplore(String datasetInstance);

  /**
   * Disable ad-hoc exploration of the given {@link com.continuuity.api.data.batch.RecordScannable}.
   *
   * @param datasetInstance dataset instance name.
   * @return a {@code Future} object that can either successfully complete, or enter a failed state depending on
   *         the success of the disable operation.
   */
  ListenableFuture<Void> disableExplore(String datasetInstance);

  /**
   * Execute a Hive SQL statement asynchronously. The returned
   * {@link com.continuuity.explore.client.StatementExecutionFuture} can be used to get the
   * schema of the operation, and it contains an iterator on the results of the statement.

   * @param statement SQL statement.
   * @return {@link com.continuuity.explore.client.StatementExecutionFuture} eventually containing
   * the results of the statement execution.
   */
  StatementExecutionFuture submit(String statement);
}
