/*
 * Copyright 2014 Cask Data, Inc.
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

package co.cask.cdap.internal.app.runtime.spark;

import co.cask.cdap.api.metrics.Metrics;
import co.cask.cdap.api.spark.SparkContext;
import co.cask.cdap.api.spark.SparkSpecification;
import co.cask.cdap.app.program.Program;
import co.cask.cdap.app.runtime.Arguments;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.logging.LoggingContext;
import co.cask.cdap.common.metrics.MetricsCollectionService;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.internal.app.runtime.AbstractContext;
import co.cask.cdap.internal.app.runtime.ProgramServiceDiscovery;
import co.cask.cdap.logging.context.SparkLoggingContext;
import com.continuuity.tephra.TransactionAware;
import com.google.common.collect.ImmutableMap;
import org.apache.twill.api.RunId;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.apache.twill.discovery.ServiceDiscovered;

import java.util.Map;
import java.util.Set;

/**
 * Spark job runtime context
 */
public class BasicSparkContext extends AbstractContext implements SparkContext {

  // todo:  REACTOR-853: "InstanceId is not supported in Spark jobs"
  public static final String INSTANCE_ID = "0";
  private final Arguments runtimeArguments;
  private final SparkSpecification spec;
  private final long logicalStartTime;
  private final String accountId;
  private final String workflowBatch;
  private final ProgramServiceDiscovery serviceDiscovery;
  private final MetricsCollectionService metricsCollectionService;
  private final SparkLoggingContext loggingContext;

  public BasicSparkContext(Program program, RunId runId, Arguments runtimeArguments, Set<String> datasets,
                           SparkSpecification spec, long logicalStartTime, String workflowBatch,
                           ProgramServiceDiscovery serviceDiscovery, MetricsCollectionService metricsCollectionService,
                           DatasetFramework dsFramework, CConfiguration conf,
                           DiscoveryServiceClient discoveryServiceClient) {
    super(program, runId, datasets, getMetricContext(program), metricsCollectionService, dsFramework, conf,
          serviceDiscovery, discoveryServiceClient);
    this.accountId = program.getAccountId();
    this.runtimeArguments = runtimeArguments;
    this.logicalStartTime = logicalStartTime;
    this.workflowBatch = workflowBatch;
    this.serviceDiscovery = serviceDiscovery;
    this.metricsCollectionService = metricsCollectionService;

    //TODO: Metrics needs to be initialized here properly when implemented.

    this.loggingContext = new SparkLoggingContext(getAccountId(), getApplicationId(), getProgramName());
    this.spec = spec;
  }

  @Override
  public String toString() {
    return String.format("job=%s,=%s", spec.getName(), super.toString());
  }

  @Override
  public SparkSpecification getSpecification() {
    return spec;
  }

  @Override
  public long getLogicalStartTime() {
    return logicalStartTime;
  }

  @Override
  public <T> T readFromDataset(String datasetName, Class<?> kClass, Class<?> vClass) {
    throw new IllegalStateException("Reading dataset is not supported here");
  }

  @Override
  public <T> void writeToDataset(T rdd, String datasetName, Class<?> kClass, Class<?> vClass) {
    throw new IllegalStateException("Writing  dataset is not supported here");
  }

  private static String getMetricContext(Program program) {
    return String.format("%s.b.%s.%s", program.getApplicationId(), program.getName(), INSTANCE_ID);
  }

  @Override
  public <T> T getBaseSparkContext() {
    throw new IllegalStateException("Getting base Spark Context is not supported here");
  }

  @Override
  public Map<String, String> getRuntimeArguments() {
    ImmutableMap.Builder<String, String> arguments = ImmutableMap.builder();
    for (Map.Entry<String, String> runtimeArgument : runtimeArguments) {
      arguments.put(runtimeArgument);
    }
    return arguments.build();
  }

  @Override
  public ServiceDiscovered discover(String applicationId, String serviceId, String serviceName) {
    //TODO: Change this once we start supporting services in Spark.
    throw new UnsupportedOperationException("Service Discovery not supported");
  }

  //TODO: Change this once we have metrics is supported
  @Override
  public Metrics getMetrics() {
    throw new UnsupportedOperationException("Metrics are not not supported in Spark yet");
  }

  public LoggingContext getLoggingContext() {
    return loggingContext;
  }

  public void flushOperations() throws Exception {
    for (TransactionAware txAware : getDatasetInstantiator().getTransactionAware()) {
      txAware.commitTx();
    }
  }

  public Arguments getRuntimeArgs() {
    return runtimeArguments;
  }
}
