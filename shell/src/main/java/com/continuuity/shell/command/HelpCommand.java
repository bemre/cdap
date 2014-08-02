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

package com.continuuity.shell.command;

import com.continuuity.shell.ReactorShellConfig;
import com.continuuity.shell.ReactorShellMain;
import com.google.common.base.Supplier;

import java.io.PrintStream;

/**
 * Prints helper text for all commands.
 */
public class HelpCommand extends AbstractCommand {

  private final Supplier<CommandSet> getCommands;
  private final ReactorShellConfig config;

  public HelpCommand(Supplier<CommandSet> getCommands, ReactorShellConfig config) {
    super("help", null, "Prints this helper text");
    this.getCommands = getCommands;
    this.config = config;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    output.println("Reactor shell version " + config.getVersion());
    output.println("REACTOR_HOST=" + config.getReactorHost());
    output.println();
    output.println("Available commands: \n" + getCommands.get().getHelperText(""));
  }
}
