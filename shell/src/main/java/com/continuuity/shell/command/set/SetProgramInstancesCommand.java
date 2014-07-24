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

package com.continuuity.shell.command.set;

import com.continuuity.client.ProgramClient;
import com.continuuity.shell.ElementType;
import com.continuuity.shell.ProgramIdCompleterFactory;
import com.continuuity.shell.command.AbstractCommand;
import com.continuuity.shell.completer.Completable;
import com.google.common.collect.Lists;
import jline.console.completer.Completer;

import java.io.PrintStream;
import java.util.List;
import javax.inject.Inject;

/**
 * Sets the instances of a program.
 */
public class SetProgramInstancesCommand extends AbstractCommand implements Completable {

  private final ProgramClient programClient;
  private final ProgramIdCompleterFactory completerFactory;
  private final ElementType elementType;

  @Inject
  public SetProgramInstancesCommand(ElementType elementType,
                                    ProgramIdCompleterFactory completerFactory,
                                    ProgramClient programClient) {
    super(elementType.getName(), "<program-id> <num-instances>",
          "Sets the instances of a " + elementType.getPrettyName());
    this.elementType = elementType;
    this.completerFactory = completerFactory;
    this.programClient = programClient;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    super.process(args, output);

    String[] programIdParts = args[0].split("\\.");
    String appId = programIdParts[0];
    int numInstances = Integer.parseInt(args[1]);

    switch (elementType) {
      case FLOWLET:
        String flowId = programIdParts[1];
        String flowletId = programIdParts[2];
        programClient.setFlowletInstances(appId, flowId, flowletId, numInstances);
        break;
      case PROCEDURE:
        String procedureId = programIdParts[1];
        programClient.setProcedureInstances(appId, procedureId, numInstances);
        break;
      case RUNNABLE:
        String serviceId = programIdParts[1];
        String runnableId = programIdParts[2];
        programClient.setServiceRunnableInstances(appId, serviceId, runnableId, numInstances);
        break;
      default:
        // TODO: remove this
        throw new IllegalArgumentException("Unrecognized program element type for scaling: " + elementType);
    }
  }

  @Override
  public List<? extends Completer> getCompleters(String prefix) {
    return Lists.newArrayList(prefixCompleter(prefix, completerFactory.getProgramIdCompleter(elementType)));
  }
}
