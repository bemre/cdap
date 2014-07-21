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

package com.continuuity.data.stream;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.common.twill.AbstractDistributedReactorServiceManager;
import com.google.inject.Inject;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.discovery.DiscoveryServiceClient;

/**
 * Stream Reactor Service management in distributed mode.
 */
public class StreamServiceManager extends AbstractDistributedReactorServiceManager {


  @Inject
  public StreamServiceManager(CConfiguration cConf, TwillRunnerService twillRunnerService,
                              DiscoveryServiceClient discoveryServiceClient) {
    super(cConf, Constants.Service.STREAMS, twillRunnerService, discoveryServiceClient);
  }

  @Override
  public int getMaxInstances() {
    return cConf.getInt(Constants.Stream.MAX_INSTANCES);
  }

  @Override
  public String getDescription() {
    return Constants.Stream.SERVICE_DESCRIPTION;
  }

}