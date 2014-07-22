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

import com.continuuity.reactor.client.common.ReactorClientTestBase;
import com.continuuity.reactor.client.config.ReactorClientConfig;
import com.continuuity.reactor.metadata.SystemServiceMeta;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class ReactorMonitorClientTest extends ReactorClientTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(ReactorMonitorClientTest.class);

  private ReactorMonitorClient monitorClient;

  @Before
  public void setUp() throws Throwable {
    super.setUp();

    monitorClient = new ReactorMonitorClient(new ReactorClientConfig("localhost"));
  }

  @Test
  public void testAll() throws Exception {
    List<SystemServiceMeta> services = monitorClient.listSystemServices();
    Assert.assertTrue(services.size() > 0);

    String someService = services.get(0).getName();
    String serviceStatus = monitorClient.getSystemServiceStatus(someService);
    Assert.assertEquals("OK", serviceStatus);

    int systemServiceInstances = monitorClient.getSystemServiceInstances(someService);
    monitorClient.setSystemServiceInstances(someService, 3);
  }
}
