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
package co.cask.cdap.examples.purchase;

import co.cask.cdap.api.annotation.Handle;
import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.procedure.AbstractProcedure;
import co.cask.cdap.api.procedure.ProcedureContext;
import co.cask.cdap.api.procedure.ProcedureRequest;
import co.cask.cdap.api.procedure.ProcedureResponder;
import co.cask.cdap.api.procedure.ProcedureResponse;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A Procedure for querying the history DataSet for a customer's purchase history.
 */
public class PurchaseProcedure extends AbstractProcedure {

  @UseDataSet("history")
  private PurchaseHistoryStore store;
  private URL serviceURL;

  @Override
  public void initialize(ProcedureContext context) {
    //Discover the CatalogLookup service via discovery service
    // the service name is the same as the one provided in the Application configure method
    serviceURL = context.getServiceURL("PurchaseHistory", PurchaseApp.SERVICE_NAME);
  }

  /**
   *
   * @param request The request, which must contain the "product" argument.
   *        Example: Looking up the product-id of a comb: queryCatalogLookup({"product":"comb"}).
   *
   */
  @Handle("catalogLookup")
  @SuppressWarnings("unused")
  public void catalogLookup(ProcedureRequest request, ProcedureResponder responder) throws Exception {
    String productId = null;
    String product = request.getArgument("product");
    if (product == null) {
      responder.error(ProcedureResponse.Code.CLIENT_ERROR, "Product must be given as argument");
    }
    if (serviceURL == null) {
      responder.error(ProcedureResponse.Code.NOT_FOUND, "serviceURL is null");
    }
    try {
      URL url = new URL(serviceURL, String.format("v1/product/%s/catalog", product));
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      if (HttpURLConnection.HTTP_OK == conn.getResponseCode()) {
        try {
          productId = new String(ByteStreams.toByteArray(conn.getInputStream()), Charsets.UTF_8);
        } finally {
          conn.disconnect();
        }
      }
    } catch (Throwable th) {
    }

    if (productId == null) {
      responder.error(ProcedureResponse.Code.NOT_FOUND, "No product-id found for " + product);
    } else {
      responder.sendJson(new ProcedureResponse(ProcedureResponse.Code.SUCCESS), productId);
    }
  }

  @UseDataSet("testTable")
  private KeyValueTable table;

  /**
   * Return the specified customer's purchases as a JSON history object.
   *
   * @param request The request, which must contain the "customer" argument. 
   *        Example: history method with a request parameter for a customer named Tom: history({"customer":"Tom"}).
   */
  @Handle("history")
  @SuppressWarnings("unused")
  public void history(ProcedureRequest request, ProcedureResponder responder) throws Exception {
    String customer = request.getArgument("customer");
    if (customer == null) {
      responder.error(ProcedureResponse.Code.CLIENT_ERROR, "Customer must be given as argument");
      return;
    }
    PurchaseHistory history = store.read(customer);
    if (history == null) {
      responder.error(ProcedureResponse.Code.NOT_FOUND, "No purchase history found for " + customer);
    } else {
      responder.sendJson(new ProcedureResponse(ProcedureResponse.Code.SUCCESS), history);
    }
  }

  @Handle("ping")
  @SuppressWarnings("unused")
  public void ping(ProcedureRequest request, ProcedureResponder responder) throws Exception {
    String key = request.getArgument("key");
    if (key == null) {
      responder.error(ProcedureResponse.Code.NOT_FOUND, "Key not provided.");
      return;
    }

    int value = Bytes.toInt(table.read(key));
    responder.sendJson(ProcedureResponse.Code.SUCCESS, value);
    return;
  }

}
