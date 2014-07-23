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

package com.continuuity.reactor.shell.command.truncate;

import com.continuuity.reactor.client.ReactorDatasetClient;
import com.continuuity.reactor.client.ReactorStreamClient;
import com.continuuity.reactor.shell.ProgramIdCompleterFactory;
import com.continuuity.reactor.shell.command.Command;
import com.continuuity.reactor.shell.command.CommandSet;
import com.continuuity.reactor.shell.completer.reactor.DatasetNameCompleter;
import com.continuuity.reactor.shell.completer.reactor.StreamIdCompleter;
import com.google.common.collect.Lists;

import javax.inject.Inject;

/**
 * Contains commands for truncating stuff.
 */
public class TruncateCommandSet extends CommandSet {

  @Inject
  public TruncateCommandSet(DatasetNameCompleter datasetNameCompleter, StreamIdCompleter streamIdCompleter,
                            ReactorDatasetClient datasetClient, ReactorStreamClient streamClient) {
    super("truncate", Lists.<Command>newArrayList(
      new TruncateDatasetCommand(datasetNameCompleter, datasetClient),
      new TruncateStreamCommand(streamIdCompleter, streamClient)
    ));
  }
}