/*
 * Copyright 2014 Continuuity, Inc.
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

package com.continuuity.data2.datafabric.dataset.service;

import com.continuuity.api.dataset.Dataset;
import com.continuuity.api.dataset.DatasetAdmin;
import com.continuuity.api.dataset.DatasetDefinition;
import com.continuuity.api.dataset.DatasetProperties;
import com.continuuity.api.dataset.DatasetSpecification;
import com.continuuity.api.dataset.lib.AbstractDatasetDefinition;
import com.continuuity.api.dataset.lib.CompositeDatasetAdmin;
import com.continuuity.api.dataset.module.DatasetDefinitionRegistry;
import com.continuuity.api.dataset.module.DatasetModule;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Test dataset module
 */
public class TestModule2 implements DatasetModule {
  @Override
  public void register(DatasetDefinitionRegistry registry) {
    registry.get("datasetType1");
    registry.add(createDefinition("datasetType2"));
  }

  private DatasetDefinition createDefinition(String name) {
    return new AbstractDatasetDefinition(name) {
      @Override
      public DatasetSpecification configure(String instanceName, DatasetProperties properties) {
        return createSpec(instanceName, getName(), properties);
      }

      @Override
      public DatasetAdmin getAdmin(DatasetSpecification spec, ClassLoader classLoader) {
        return new CompositeDatasetAdmin(Collections.<DatasetAdmin>emptyList());
      }

      @Override
      public Dataset getDataset(DatasetSpecification spec, Map arguments, ClassLoader classLoader) throws IOException {
        return null;
      }
    };
  }
  private DatasetSpecification createSpec(String instanceName, String typeName,
                                          DatasetProperties properties) {
    return DatasetSpecification.builder(instanceName, typeName).properties(properties.getProperties()).build();
  }
}
