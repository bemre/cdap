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

package co.cask.cdap.data2.datafabric.dataset.service;

import co.cask.cdap.api.dataset.module.DatasetModule;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.http.HttpRequest;
import co.cask.cdap.common.http.HttpRequests;
import co.cask.cdap.common.http.ObjectResponse;
import co.cask.cdap.common.lang.jar.JarFinder;
import co.cask.cdap.common.metrics.MetricsCollectionService;
import co.cask.cdap.common.metrics.NoOpMetricsCollectionService;
import co.cask.cdap.data2.datafabric.dataset.InMemoryDefinitionRegistryFactory;
import co.cask.cdap.data2.datafabric.dataset.RemoteDatasetFramework;
import co.cask.cdap.data2.datafabric.dataset.instance.DatasetInstanceManager;
import co.cask.cdap.data2.datafabric.dataset.service.executor.InMemoryDatasetOpExecutor;
import co.cask.cdap.data2.datafabric.dataset.service.mds.MDSDatasetsRegistry;
import co.cask.cdap.data2.datafabric.dataset.type.DatasetTypeManager;
import co.cask.cdap.data2.datafabric.dataset.type.LocalDatasetTypeClassLoaderFactory;
import co.cask.cdap.data2.dataset2.InMemoryDatasetFramework;
import co.cask.cdap.data2.dataset2.module.lib.inmemory.InMemoryOrderedTableModule;
import co.cask.cdap.explore.client.DatasetExploreFacade;
import co.cask.cdap.explore.client.DiscoveryExploreClient;
import co.cask.cdap.proto.DatasetModuleMeta;
import com.continuuity.tephra.TransactionManager;
import com.continuuity.tephra.inmemory.InMemoryTxSystemClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gson.reflect.TypeToken;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.twill.discovery.InMemoryDiscoveryService;
import org.apache.twill.filesystem.LocalLocationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Base class for unit-tests that require running of {@link DatasetService}
 */
public abstract class DatasetServiceTestBase {
  private int port;
  private DatasetService service;
  protected TransactionManager txManager;
  protected RemoteDatasetFramework dsFramework;

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    CConfiguration cConf = CConfiguration.create();
    File datasetDir = new File(tmpFolder.newFolder(), "dataset");
    if (!datasetDir.mkdirs()) {
      throw
        new RuntimeException(String.format("Could not create DatasetFramework output dir %s", datasetDir.getPath()));
    }
    cConf.set(Constants.Dataset.Manager.OUTPUT_DIR, datasetDir.getAbsolutePath());
    cConf.set(Constants.Dataset.Manager.ADDRESS, "localhost");
    cConf.setBoolean(Constants.Dangerous.UNRECOVERABLE_RESET, true);

    // Starting DatasetService service
    InMemoryDiscoveryService discoveryService = new InMemoryDiscoveryService();
    MetricsCollectionService metricsCollectionService = new NoOpMetricsCollectionService();

    // Tx Manager to support working with datasets
    Configuration txConf = HBaseConfiguration.create();
    cConf.copyTxProperties(txConf);
    txManager = new TransactionManager(txConf);
    txManager.startAndWait();
    InMemoryTxSystemClient txSystemClient = new InMemoryTxSystemClient(txManager);

    LocalLocationFactory locationFactory = new LocalLocationFactory();
    dsFramework = new RemoteDatasetFramework(discoveryService, locationFactory, new InMemoryDefinitionRegistryFactory(),
                                             new LocalDatasetTypeClassLoaderFactory());

    ImmutableMap<String, ? extends DatasetModule> defaultModules =
      ImmutableMap.of("memoryTable", new InMemoryOrderedTableModule());

    MDSDatasetsRegistry mdsDatasetsRegistry =
      new MDSDatasetsRegistry(txSystemClient, defaultModules,
                              new InMemoryDatasetFramework(new InMemoryDefinitionRegistryFactory()), cConf);

    service = new DatasetService(cConf,
                                 locationFactory,
                                 discoveryService,
                                 new DatasetTypeManager(mdsDatasetsRegistry, locationFactory,
                                                        // we don't need any default modules in this test
                                                        Collections.<String, DatasetModule>emptyMap()),
                                 new DatasetInstanceManager(mdsDatasetsRegistry),
                                 metricsCollectionService,
                                 new InMemoryDatasetOpExecutor(dsFramework),
                                 mdsDatasetsRegistry,
                                 new DatasetExploreFacade(new DiscoveryExploreClient(discoveryService), cConf));
    service.startAndWait();
    port = discoveryService.discover(Constants.Service.DATASET_MANAGER).iterator().next().getSocketAddress().getPort();
  }

  @After
  public void after() {
    try {
      service.stopAndWait();
    } finally {
      txManager.stopAndWait();
    }
  }

  protected URL getUrl(String resource) throws MalformedURLException {
    return new URL("http://" + "localhost" + ":" + port + Constants.Gateway.GATEWAY_VERSION + resource);
  }

  protected int deployModule(String moduleName, Class moduleClass) throws Exception {
    String jarPath = JarFinder.getJar(moduleClass);
    final FileInputStream is = new FileInputStream(jarPath);
    try {
      HttpRequest request = HttpRequest.put(getUrl("/data/modules/" + moduleName))
        .addHeader("X-Continuuity-Class-Name", moduleClass.getName())
        .withBody(new File(jarPath)).build();
      return HttpRequests.execute(request).getResponseCode();
    } finally {
      is.close();
    }
  }

  // creates a bundled jar with moduleClass and list of bundleEmbeddedJar files, moduleName and moduleClassName are
  // used to make request for deploying module.
  protected int deployModuleBundled(String moduleName, String moduleClassName, Class moduleClass,
                                    File...bundleEmbeddedJars) throws IOException {
    String jarPath = JarFinder.getJar(moduleClass);
    JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(jarPath));
    try {
      for (File embeddedJar : bundleEmbeddedJars) {
        JarEntry jarEntry = new JarEntry("lib/" + embeddedJar.getName());
        jarOutput.putNextEntry(jarEntry);
        Files.copy(embeddedJar, jarOutput);
      }
    } finally {
      jarOutput.close();
    }
    final FileInputStream is = new FileInputStream(jarPath);
    try {
      HttpRequest request = HttpRequest.put(getUrl("/data/modules/" + moduleName))
        .addHeader("X-Continuuity-Class-Name", moduleClassName)
        .withBody(new File(jarPath)).build();
      return HttpRequests.execute(request).getResponseCode();
    } finally {
      is.close();
    }
  }

  protected ObjectResponse<List<DatasetModuleMeta>> getModules() throws IOException {
    return ObjectResponse.fromJsonBody(HttpRequests.execute(HttpRequest.get(getUrl("/data/modules")).build()),
                                       new TypeToken<List<DatasetModuleMeta>>() { }.getType());
  }

  protected int deleteInstances() throws IOException {
    return HttpRequests.execute(HttpRequest.delete(getUrl("/data/unrecoverable/datasets")).build()).getResponseCode();
  }

  protected int deleteModule(String moduleName) throws Exception {
    return HttpRequests.execute(HttpRequest.delete(getUrl("/data/modules/" + moduleName)).build()).getResponseCode();
  }

  protected int deleteModules() throws IOException {
    return HttpRequests.execute(HttpRequest.delete(getUrl("/data/modules/")).build()).getResponseCode();
  }
}
