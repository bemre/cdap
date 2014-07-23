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

import com.continuuity.reactor.client.ReactorAppClient;
import com.continuuity.reactor.metadata.ApplicationRecord;
import com.continuuity.reactor.shell.completer.StringsCompleter;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

/**
 * Completer for application IDs.
 */
public class AppIdCompleter extends StringsCompleter {

  private static final Logger LOG = LoggerFactory.getLogger(AppIdCompleter.class);

  @Inject
  public AppIdCompleter(final ReactorAppClient reactorAppClient) {
    super(new Supplier<Collection<String>>() {
      @Override
      public Collection<String> get() {
        try {
          List<ApplicationRecord> appsList = reactorAppClient.list();
          List<String> appIds = Lists.newArrayList();
          for (ApplicationRecord item : appsList) {
            appIds.add(item.getId());
          }
          return appIds;
        } catch (IOException e) {
//          LOG.error("Error retrieving list of apps for autocompletion", e);
          return Lists.newArrayList();
        }
      }
    });
  }
}