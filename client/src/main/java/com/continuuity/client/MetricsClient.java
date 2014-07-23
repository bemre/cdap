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

package com.continuuity.client;

import com.continuuity.client.config.ReactorClientConfig;
import com.continuuity.client.util.RestClient;
import com.continuuity.common.http.HttpMethod;
import com.continuuity.common.http.HttpResponse;
import com.continuuity.common.http.ObjectResponse;

import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;

/**
 * Provides ways to interact with Reactor metrics.
 */
public class MetricsClient {

  private final RestClient restClient;
  private final ReactorClientConfig config;

  @Inject
  public MetricsClient(ReactorClientConfig config) {
    this.config = config;
    this.restClient = RestClient.create(config);
  }

  /**
   * Gets the value of a particular metric.
   *
   * @param scope scope of the metric
   * @param context context of the metric
   * @param metric name of the metric
   * @param timeRange time range to query
   * @return value of the metric
   * @throws IOException if a network error occurred
   */
  public int getMetric(String scope, String context, String metric, String timeRange) throws IOException {
    URL url = config.resolveURL(String.format("metrics/%s/%s/%s?%s", scope, context, metric, timeRange));
    HttpResponse response = restClient.execute(HttpMethod.GET, url);
    return ObjectResponse.fromJsonBody(response, Integer.class).getResponseObject();
  }

}