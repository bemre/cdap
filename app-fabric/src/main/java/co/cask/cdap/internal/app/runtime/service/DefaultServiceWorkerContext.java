/*
 * Copyright 2014 Cask, Inc.
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

package co.cask.cdap.internal.app.runtime.service;

import co.cask.cdap.api.data.DataSetContext;
import co.cask.cdap.api.data.DataSetInstantiationException;
import co.cask.cdap.api.dataset.lib.AbstractDataset;
import co.cask.cdap.api.service.ServiceWorker;
import co.cask.cdap.api.service.ServiceWorkerContext;
import co.cask.cdap.api.service.TxRunnable;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import com.continuuity.tephra.TransactionContext;
import com.continuuity.tephra.TransactionFailureException;
import com.continuuity.tephra.TransactionSystemClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.twill.discovery.ServiceDiscovered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link ServiceWorkerContext}.
 */
public class DefaultServiceWorkerContext implements ServiceWorkerContext {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultServiceWorkerContext.class);
  private final Map<String, String> runtimeArgs;
  private final TransactionSystemClient transactionSystemClient;
  private final DatasetFramework datasetFramework;

  /**
   * Create a ServiceWorkerContext with runtime arguments.
   * @param runtimeArgs for the worker.
   */
  public DefaultServiceWorkerContext(Map<String, String> runtimeArgs,
                                     TransactionSystemClient transactionSystemClient,
                                     DatasetFramework datasetFramework) {
    this.runtimeArgs = ImmutableMap.copyOf(runtimeArgs);
    this.transactionSystemClient = transactionSystemClient;
    this.datasetFramework = datasetFramework;
  }

  @Override
  public Map<String, String> getRuntimeArguments() {
    return runtimeArgs;
  }

  @Override
  public ServiceDiscovered discover(String applicationId, String serviceId, String serviceName) {
    throw new UnsupportedOperationException("Service discovery not yet supported.");
  }

  @Override
  public URL getServiceURL(String applicationId, String serviceId) {
    throw new UnsupportedOperationException("Service discovery not yet supported.");
  }

  @Override
  public URL getServiceURL(String serviceId) {
    throw new UnsupportedOperationException("Service discovery not yet supported.");
  }

  @Override
  public void execute(TxRunnable runnable) {
    final TransactionContext context = new TransactionContext(transactionSystemClient);
    try {
      context.start();
      runnable.run(new DataSetContext() {
        @Override
        public <T extends Closeable> T getDataSet(String name) throws DataSetInstantiationException {
          try {
            AbstractDataset dataset = datasetFramework.getDataset("cdap.user." + name,
                                                                  ImmutableMap.<String, String>of(),
                                                                  getClass().getClassLoader());
            context.addTransactionAware(dataset);
            return (T) dataset;
          } catch (Exception e) {
            try {
              context.abort();
            } catch (TransactionFailureException e1) {

            }
          }
          return null;
        }

        @Override
        public <T extends Closeable> T getDataSet(String name, Map<String, String> arguments)
          throws DataSetInstantiationException {
          return null;
        }
      });
      context.finish();
    } catch (Exception e) {

    }
  }
}
