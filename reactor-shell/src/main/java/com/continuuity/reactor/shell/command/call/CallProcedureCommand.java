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

package com.continuuity.reactor.shell.command.call;

import com.continuuity.reactor.client.ReactorProcedureClient;
import com.continuuity.reactor.shell.ProgramElementType;
import com.continuuity.reactor.shell.ProgramIdCompleterFactory;
import com.continuuity.reactor.shell.command.AbstractCommand;
import com.continuuity.reactor.shell.completer.Completable;
import com.continuuity.reactor.shell.exception.CommandInputError;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jline.console.completer.Completer;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Calls a procedure.
 */
public class CallProcedureCommand extends AbstractCommand implements Completable {

  private final ReactorProcedureClient reactorProcedureClient;
  private final ProgramIdCompleterFactory programIdCompleterFactory;

  public CallProcedureCommand(ProgramIdCompleterFactory programIdCompleterFactory,
                              ReactorProcedureClient reactorProcedureClient) {
    super("procedure", "<app-id>.<procedure-id> <method-id> <parameters-map>", "Calls a procedure");
    this.programIdCompleterFactory = programIdCompleterFactory;
    this.reactorProcedureClient = reactorProcedureClient;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    if (argsFormat != null && args.length < 2) {
      throw new CommandInputError("Expected arguments: " + argsFormat);
    }

    String[] programIdParts = args[0].split("\\.");
    String appId = programIdParts[0];
    String procedureId = programIdParts[1];
    String methodId = args[1];
    String[] parameters = Arrays.copyOfRange(args, 2, args.length);
    Map<String, String> parametersMap = Maps.newHashMap();

    for (int i = 0; i < parameters.length; i += 2) {
      String key = parameters[i];
      String value = parameters[i + 1];
      parametersMap.put(key, value);
    }

    String result = reactorProcedureClient.call(appId, procedureId, methodId, parametersMap);
    output.println(result);
  }

  @Override
  public List<? extends Completer> getCompleters(String prefix) {
    return Lists.newArrayList(
      prefixCompleter(prefix, programIdCompleterFactory.getProgramIdCompleter(ProgramElementType.PROCEDURE)));
  }
}