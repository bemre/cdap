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

package com.continuuity.reactor.shell.completer.reactor;

import com.continuuity.reactor.client.ReactorStreamClient;
import com.continuuity.reactor.metadata.StreamRecord;
import com.continuuity.reactor.shell.completer.StringsCompleter;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

/**
 * Completer for stream IDs.
 */
public class StreamIdCompleter extends StringsCompleter {

  private static final Logger LOG = LoggerFactory.getLogger(StreamIdCompleter.class);

  @Inject
  public StreamIdCompleter(final ReactorStreamClient reactorStreamClient) {
    super(new Supplier<Collection<String>>() {
      @Override
      public Collection<String> get() {
        try {
          List<StreamRecord> list = reactorStreamClient.list();
          return Lists.newArrayList(
            Iterables.transform(list, new Function<StreamRecord, String>() {
              @Override
              public String apply(StreamRecord input) {
                return input.getId();
              }
            })
          );
        } catch (IOException e) {
//          LOG.error("Error retrieving stream id list for autocompletion", e);
          return Lists.newArrayList();
        }
      }
    });
  }
}