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

package com.continuuity.reactor.client;

import com.continuuity.client.DatasetClient;
import com.continuuity.client.ReactorDatasetModuleClient;
import com.continuuity.client.ReactorDatasetTypeClient;
import com.continuuity.proto.DatasetTypeMeta;
import com.continuuity.reactor.client.app.FakeDataset;
import com.continuuity.reactor.client.app.FakeDatasetModule;
import com.continuuity.reactor.client.common.ReactorClientTestBase;
import com.continuuity.client.config.ReactorClientConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 *
 */
public class DatasetClientTest extends ReactorClientTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetClientTest.class);

  private DatasetClient datasetClient;
  private ReactorDatasetModuleClient moduleClient;
  private ReactorDatasetTypeClient typeClient;

  @Before
  public void setUp() throws Throwable {
    super.setUp();

    ReactorClientConfig config = new ReactorClientConfig("localhost");
    datasetClient = new DatasetClient(config);
    moduleClient = new ReactorDatasetModuleClient(config);
    typeClient = new ReactorDatasetTypeClient(config);
  }

  @Test
  public void testAll() throws Exception {
    int numBaseModules = moduleClient.list().size();
    int numBaseTypes = typeClient.list().size();
    Assert.assertEquals(0, datasetClient.list().size());

    LOG.info("Adding dataset module");
    File moduleJarFile = createAppJarFile(FakeDatasetModule.class);
    moduleClient.add(FakeDatasetModule.NAME, FakeDatasetModule.class.getName(), moduleJarFile);
    Assert.assertEquals(numBaseModules + 1, moduleClient.list().size());
    Assert.assertEquals(numBaseTypes + 2, typeClient.list().size());

    LOG.info("Checking new dataset type exists");
    DatasetTypeMeta datasetTypeMeta = typeClient.get(FakeDataset.TYPE_NAME);
    Assert.assertNotNull(datasetTypeMeta);
    Assert.assertEquals(FakeDataset.TYPE_NAME, datasetTypeMeta.getName());

    datasetTypeMeta = typeClient.get(FakeDataset.class.getName());
    Assert.assertNotNull(datasetTypeMeta);
    Assert.assertEquals(FakeDataset.class.getName(), datasetTypeMeta.getName());

    LOG.info("Creating, truncating, and deleting dataset of new dataset type");
    Assert.assertEquals(0, datasetClient.list().size());
    datasetClient.create("testDataset", FakeDataset.TYPE_NAME);
    Assert.assertEquals(1, datasetClient.list().size());
    datasetClient.truncate("testDataset");
    datasetClient.delete("testDataset");
    Assert.assertEquals(0, datasetClient.list().size());

    LOG.info("Creating and deleting multiple datasets");
    datasetClient.create("testDataset1", FakeDataset.TYPE_NAME);
    datasetClient.create("testDataset2", FakeDataset.TYPE_NAME);
    datasetClient.create("testDataset3", FakeDataset.TYPE_NAME);
    Assert.assertEquals(3, datasetClient.list().size());
    datasetClient.deleteAll();
    Assert.assertEquals(0, datasetClient.list().size());

    LOG.info("Deleting dataset module");
    moduleClient.delete(FakeDatasetModule.NAME);
    Assert.assertEquals(numBaseModules, moduleClient.list().size());
    Assert.assertEquals(numBaseTypes, typeClient.list().size());

    LOG.info("Adding dataset module and then deleting all dataset modules");
    moduleClient.add("testModule1", FakeDatasetModule.class.getName(), moduleJarFile);
    Assert.assertEquals(numBaseModules + 1, moduleClient.list().size());
    Assert.assertEquals(numBaseTypes + 2, typeClient.list().size());
    moduleClient.deleteAll();
    Assert.assertEquals(numBaseModules, moduleClient.list().size());
    Assert.assertEquals(numBaseTypes, typeClient.list().size());
  }
}
