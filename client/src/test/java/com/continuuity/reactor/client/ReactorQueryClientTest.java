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

import com.continuuity.proto.ColumnDesc;
import com.continuuity.proto.QueryHandle;
import com.continuuity.proto.QueryResult;
import com.continuuity.proto.QueryStatus;
import com.continuuity.reactor.client.app.FakeApp;
import com.continuuity.reactor.client.common.ReactorClientTestBase;
import com.continuuity.reactor.client.config.ReactorClientConfig;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class ReactorQueryClientTest extends ReactorClientTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(ReactorQueryClientTest.class);

  private ReactorAppClient appClient;
  private ReactorQueryClient queryClient;

  @Before
  public void setUp() throws Throwable {
    super.setUp();

    ReactorClientConfig config = new ReactorClientConfig("localhost");
    appClient = new ReactorAppClient(config);
    queryClient = new ReactorQueryClient(config);
  }

//  @Test
  public void testAll() throws Exception {
    appClient.deploy(createAppJarFile(FakeApp.class));

    QueryHandle queryHandle = queryClient.execute("select * from continuuity_user_" + FakeApp.DS_NAME);
    QueryStatus status = new QueryStatus(null, false);

    long startTime = System.currentTimeMillis();
    // TODO: refactor
    while ("RUNNING".equals(status.getStatus()) || "INITIALIZED".equals(status.getStatus())
      || "PENDING".equals(status.getStatus())) {
      Thread.sleep(1000);
      status = queryClient.getStatus(queryHandle);
    }

    Assert.assertTrue(status.hasResults());

    List<ColumnDesc> schema = queryClient.getSchema(queryHandle);
    String[] header = new String[schema.size()];
    for (int i = 0; i < header.length; i++) {
      ColumnDesc column = schema.get(i);
      // Hive columns start at 1
      int index = column.getPosition() - 1;
      header[index] = column.getName() + ": " + column.getType();
    }
    List<QueryResult> results = queryClient.getResults(queryHandle, 20);

    queryClient.delete(queryHandle);
  }
}
