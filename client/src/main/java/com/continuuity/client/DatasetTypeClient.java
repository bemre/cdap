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

package com.continuuity.client;

import com.continuuity.client.config.ReactorClientConfig;
import com.continuuity.client.util.RestClient;
import com.continuuity.common.http.HttpMethod;
import com.continuuity.common.http.HttpResponse;
import com.continuuity.common.http.ObjectResponse;
import com.continuuity.proto.DatasetTypeMeta;
import com.google.common.reflect.TypeToken;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;

/**
 * Provides ways to interact with Reactor dataset types.
 */
public class DatasetTypeClient {

  private final RestClient restClient;
  private final ReactorClientConfig config;

  @Inject
  public DatasetTypeClient(ReactorClientConfig config) {
    this.config = config;
    this.restClient = RestClient.create(config);
  }

  /**
   * Lists all dataset types.
   *
   * @return list of {@link DatasetTypeMeta}s.
   * @throws IOException if a network error occurred
   */
  public List<DatasetTypeMeta> list() throws IOException {
    URL url = config.resolveURL("data/types");
    HttpResponse response = restClient.execute(HttpMethod.GET, url);
    return ObjectResponse.fromJsonBody(response, new TypeToken<List<DatasetTypeMeta>>() { }).getResponseObject();
  }

  /**
   * Gets information about a dataset type.
   *
   * @param typeName name of the dataset type
   * @return {@link DatasetTypeMeta} of the dataset type
   * @throws IOException if a network error occurred
   */
  public DatasetTypeMeta get(String typeName) throws IOException {
    URL url = config.resolveURL(String.format("data/types/%s", typeName));
    HttpResponse response = restClient.execute(HttpMethod.GET, url);
    return ObjectResponse.fromJsonBody(response, DatasetTypeMeta.class).getResponseObject();
  }

}
