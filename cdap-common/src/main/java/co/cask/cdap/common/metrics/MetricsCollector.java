/*
 * Copyright © 2014 Cask Data, Inc.
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
package co.cask.cdap.common.metrics;

/**
 * A MetricCollector allows client publish counter metrics.
 */
public interface MetricsCollector {

  /**
   * Log a metric value at the current time.
   * @param metricName Name of the metric.
   * @param value value of the metric.
   * @param tags Tags associated with the metric.
   */
  void increment(String metricName, int value, String... tags);
}
