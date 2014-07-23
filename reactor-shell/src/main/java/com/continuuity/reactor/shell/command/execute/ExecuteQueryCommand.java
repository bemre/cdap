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

package com.continuuity.reactor.shell.command.execute;

import com.continuuity.reactor.client.ReactorQueryClient;
import com.continuuity.reactor.metadata.ColumnDesc;
import com.continuuity.reactor.metadata.QueryHandle;
import com.continuuity.reactor.metadata.QueryResult;
import com.continuuity.reactor.metadata.QueryStatus;
import com.continuuity.reactor.shell.command.AbstractCommand;
import com.continuuity.reactor.shell.util.AsciiTable;
import com.continuuity.reactor.shell.util.RowMaker;
import com.google.common.base.Joiner;
import com.google.inject.Inject;

import java.io.PrintStream;
import java.util.List;

/**
 * Executes a dataset query.
 */
public class ExecuteQueryCommand extends AbstractCommand {

  private final ReactorQueryClient reactorQueryClient;

  @Inject
  public ExecuteQueryCommand(ReactorQueryClient reactorQueryClient) {
    super("execute", "<query>", "Executes a dataset query");
    this.reactorQueryClient = reactorQueryClient;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    super.process(args, output);

    String query = Joiner.on(" ").join(args);
    QueryHandle queryHandle = reactorQueryClient.execute(query);
    QueryStatus status = new QueryStatus(null, false);

    long startTime = System.currentTimeMillis();
    while (QueryStatus.OpStatus.RUNNING == status.getStatus() ||
      QueryStatus.OpStatus.INITIALIZED == status.getStatus() ||
      QueryStatus.OpStatus.PENDING == status.getStatus()) {

      Thread.sleep(1000);
      status = reactorQueryClient.getStatus(queryHandle);
    }

    if (status.hasResults()) {
      List<ColumnDesc> schema = reactorQueryClient.getSchema(queryHandle);
      String[] header = new String[schema.size()];
      for (int i = 0; i < header.length; i++) {
        ColumnDesc column = schema.get(i);
        // Hive columns start at 1
        int index = column.getPosition() - 1;
        header[index] = column.getName() + ": " + column.getType();
      }
      List<QueryResult> results = reactorQueryClient.getResults(queryHandle, 20);

      new AsciiTable<QueryResult>(header, results, new RowMaker<QueryResult>() {
        @Override
        public Object[] makeRow(QueryResult object) {
          return object.getColumns().toArray(new Object[object.getColumns().size()]);
        }
      }).print(output);

      reactorQueryClient.delete(queryHandle);
    } else {
      output.println("Couldn't obtain results after " + (System.currentTimeMillis() - startTime) + "ms. " +
                       "Try querying manually with handle " + queryHandle.getHandle());
    }
  }
}