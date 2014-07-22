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

package com.continuuity.reactor.shell.command;

import com.continuuity.reactor.shell.completer.Completable;
import com.continuuity.reactor.shell.completer.PrefixCompleter;
import com.continuuity.reactor.shell.completer.StringsCompleter;
import com.continuuity.reactor.shell.exception.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command representing a set of commands.
 */
public class CommandSet implements Command, Completable {

  private final Trie<String, Command> commandsMap;
  private final List<Command> commands;
  private final String name;

  public CommandSet(String name, List<Command> commands) {
    this.name = name;
    this.commands = ImmutableList.copyOf(commands);
    this.commandsMap = map(this.commands);
  }

  private Trie<String, Command> map(List<Command> commands) {
    Trie<String, Command> result = new PatriciaTrie<String, Command>(StringKeyAnalyzer.CHAR);
    for (Command command : commands) {
      result.put(command.getName(), command);
    }
    return result;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    if (args.length == 0) {
      // TODO: print help message
      output.println("Invalid command");
      return;
    }

    String commandName = args[0];

    Command command = commandsMap.get(commandName);
    if (command == null) {
      // lookup in trie as backup
      Map.Entry<String, Command> entry = commandsMap.select(commandName);
      if (entry.getValue().getName().startsWith(commandName)) {
        command = entry.getValue();
      }
    }

    if (command == null) {
      throw new InvalidCommandException(args[0]);
    }

    command.process(Arrays.copyOfRange(args, 1, args.length), output);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getHelperText(String namePrefix) {
    String realNamePrefix = (namePrefix == null ? "" : namePrefix);
    String realName = (name == null ? "" : name + " ");

    StringBuilder sb = new StringBuilder();
    for (Command command : commands) {
      sb.append(command.getHelperText(realNamePrefix + realName));
      if (!(command instanceof CommandSet)) {
        sb.append('\n');
      }
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  @Override
  public List<? extends Completer> getCompleters(String prefix) {
    String childPrefix = (prefix == null || prefix.isEmpty() ? "" : prefix + " ") + (name == null ? "" : name);
    List<Completer> completers = Lists.newArrayList();
    List<String> immediateCommands = Lists.newArrayList();

    for (Command command : commands) {
      String name = command.getName();

      if (command instanceof Completable) {
        // add nested completers
        Completable completable = (Completable) command;
        for (Completer completer : completable.getCompleters(childPrefix)) {
          completers.add(completer);
        }
      }

      // add immediate completer
      immediateCommands.add(name);
    }

    if (!childPrefix.isEmpty()) {
      completers.add(new PrefixCompleter(childPrefix, new StringsCompleter(immediateCommands)));
    } else {
      completers.add(new StringsCompleter(immediateCommands));
    }

    return Lists.newArrayList(new AggregateCompleter(completers));
  }

  public static final Builder builder(String name) {
    return new Builder(name);
  }

  /**
   * Builder for {@link CommandSet}.
   */
  public static final class Builder {
    private final String name;
    private final List<Command> commands;

    public Builder(String name) {
      this.name = name;
      this.commands = Lists.newArrayList();
    }

    public Builder addCommand(Command command) {
      commands.add(command);
      return this;
    }

    public CommandSet build() {
      return new CommandSet(name, commands);
    }
  }
}
