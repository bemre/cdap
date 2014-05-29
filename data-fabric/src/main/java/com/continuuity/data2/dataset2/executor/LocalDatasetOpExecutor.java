package com.continuuity.data2.dataset2.executor;

import com.google.inject.Inject;
import org.apache.twill.discovery.DiscoveryServiceClient;

/**
 * Executes Dataset operations
 */
public class LocalDatasetOpExecutor extends RemoteDatasetOpExecutor {

  private final DatasetOpExecutorServer executorServer;

  @Inject
  public LocalDatasetOpExecutor(DiscoveryServiceClient discoveryClient, DatasetOpExecutorServer executorServer) {
    super(discoveryClient);
    this.executorServer = executorServer;
  }

  @Override
  protected void startUp() throws Exception {
    executorServer.startAndWait();
  }

  @Override
  protected void shutDown() throws Exception {
    executorServer.stopAndWait();
  }
}
