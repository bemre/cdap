/*
 * Copyright © 2012-2014 Cask Data, Inc.
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

package co.cask.cdap.shell.completer.element;

import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.client.DatasetClient;
import co.cask.cdap.client.exception.UnAuthorizedAccessTokenException;
import co.cask.cdap.shell.completer.StringsCompleter;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

/**
 * Completer for dataset names.
 */
public class DatasetNameCompleter extends StringsCompleter {

  @Inject
  public DatasetNameCompleter(final DatasetClient datasetClient) {
    super(new Supplier<Collection<String>>() {
      @Override
      public Collection<String> get() {
        try {
          List<DatasetSpecification> list = datasetClient.list();
          return Lists.newArrayList(
            Iterables.transform(list, new Function<DatasetSpecification, String>() {
              @Override
              public String apply(DatasetSpecification input) {
                // TODO: hack to handle namespaced dataset names -- assumes there are no periods in dataset names
                String[] tokens = input.getName().split("\\.");
                return tokens[tokens.length - 1];
              }
            })
          );
        } catch (IOException e) {
          return Lists.newArrayList();
        } catch (UnAuthorizedAccessTokenException e) {
          return Lists.newArrayList();
        }
      }
    });
  }
}
