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
import com.continuuity.client.exception.BadRequestException;
import com.continuuity.client.exception.NotFoundException;
import com.continuuity.client.util.RESTClient;
import com.continuuity.common.http.HttpMethod;
import com.continuuity.common.http.HttpRequest;
import com.continuuity.common.http.HttpResponse;
import com.continuuity.common.http.ObjectResponse;
import com.continuuity.proto.ProgramRecord;
import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Provides ways to interact with Reactor Procedures.
 */
public class ProcedureClient {

  private static final Gson GSON = new Gson();

  private final RESTClient restClient;
  private final ReactorClientConfig config;

  @Inject
  public ProcedureClient(ReactorClientConfig config) {
    this.config = config;
    this.restClient = RESTClient.create(config);
  }

  /**
   * Calls a procedure's method.
   *
   * @param appId ID of the application that the procedure belongs to
   * @param procedureId ID of the procedure
   * @param methodId ID of the method belonging to the procedure
   * @param parameters parameters to pass with the procedure call
   * @return result of the procedure call
   * @throws BadRequestException if the input was bad
   * @throws NotFoundException if the application, procedure, or method could not be found
   * @throws IOException if a network error occurred
   */
  public String call(String appId, String procedureId, String methodId, Map<String, String> parameters)
    throws BadRequestException, NotFoundException, IOException {

    URL url = config.resolveURL(String.format("apps/%s/procedures/%s/methods/%s", appId, procedureId, methodId));
    HttpRequest request = HttpRequest.post(url).withBody(GSON.toJson(parameters)).build();

    HttpResponse response = restClient.execute(request, HttpURLConnection.HTTP_BAD_REQUEST,
                                               HttpURLConnection.HTTP_NOT_FOUND);
    if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
      throw new BadRequestException("The Application, Procedure and method exist, " +
                                      "but the arguments are not as expected: " + GSON.toJson(parameters));
    } else if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
      throw new NotFoundException("application or procedure or method", appId + "/" + procedureId + "/" + methodId);
    }
    return new String(response.getResponseBody(), Charsets.UTF_8);
  }

  /**
   * Lists all procedures.
   *
   * @return list of {@link ProgramRecord}s.
   * @throws IOException if a network error occurred
   */
  public List<ProgramRecord> list() throws IOException {
    URL url = config.resolveURL("procedures");
    HttpResponse response = restClient.execute(HttpMethod.GET, url);
    return ObjectResponse.fromJsonBody(response, new TypeToken<List<ProgramRecord>>() { }).getResponseObject();
  }
}
