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

package co.cask.cdap.gateway.tools;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.data2.OperationException;
import co.cask.cdap.gateway.GatewayTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Data set client.
 */
public class DataSetClientTest extends GatewayTestBase {
  private static final String HOSTNAME = "127.0.0.1";
  private static final String API_KEY = GatewayTestBase.getAuthHeader().getValue();

  @Test
  @Ignore
  public void testValueAsCounter() throws OperationException {
    CConfiguration configuration = CConfiguration.create();
    String port = Integer.toString(GatewayTestBase.getPort());

    final String table = "tVAC";
    final String row = "myRow";
    final String column = "myCounter";
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[] {
      "create", "--table", table, "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[] {
      "write", "--table", table, "--row", row, "--column", column, "--value", "41", "--counter",
      "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));
    Assert.assertEquals("42", new DataSetClient().disallowSSL().execute(new String[] {
      "increment", "--table", table, "--row", row, "--column", column, "--value", "1",
      "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));

    configuration.set(Constants.Router.ADDRESS, HOSTNAME);
    configuration.set(Constants.Router.FORWARD, port + ":" + Constants.Service.GATEWAY + ",20000:$HOST");
    Assert.assertEquals("42", new DataSetClient().disallowSSL().execute(new String[]{
      "read", "--table", table, "--row", row, "--column", column, "--counter", "--apikey", API_KEY}, configuration));
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[]{
      "delete", "--table", table, "--row", row, "--column", column, "--value", "41", "--apikey", API_KEY},
                                                           configuration));
    Assert.assertNull(new DataSetClient().disallowSSL().execute(new String[]{
      "read", "--table", table, "--row", row, "--column", column, "--counter", "--apikey", API_KEY}, configuration));
  }

  @Test
  @Ignore
  public void testClearDataset() throws OperationException {
    CConfiguration configuration = CConfiguration.create();
    String port = Integer.toString(GatewayTestBase.getPort());

    final String table = "tVACLr";
    final String row = "myRow";
    final String column = "myCounter";
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[] {
      "create", "--table", table, "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[] {
      "write", "--table", table, "--row", row, "--column", column, "--value", "41", "--counter",
      "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));
    Assert.assertEquals("42", new DataSetClient().disallowSSL().execute(new String[] {
      "increment", "--table", table, "--row", row, "--column", column, "--value", "1",
      "--host", HOSTNAME, "--port", port, "--apikey", API_KEY }, configuration));

    configuration.set(Constants.Router.ADDRESS, HOSTNAME);
    configuration.set(Constants.Router.FORWARD, port + ":" + Constants.Service.GATEWAY);
    Assert.assertEquals("OK.", new DataSetClient().disallowSSL().execute(new String[]{
      "clear", "--table", table, "--apikey", API_KEY}, configuration));
    Assert.assertNull(new DataSetClient().disallowSSL().execute(new String[]{
      "read", "--table", table, "--row", row, "--column", column, "--counter", "--apikey", API_KEY}, configuration));
  }
}
