/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.cdap.data2.util.hbase;

/**
 * Factory for HBase version-specific {@link HBaseTableUtil} instances.
 */
public class HBaseTableUtilFactory extends HBaseVersionSpecificFactory<HBaseTableUtil> {
  @Override
  protected String getHBase94Classname() {
    return "co.cask.cdap.data2.util.hbase.HBase94TableUtil";
  }

  @Override
  protected String getHBase96Classname() {
    return "co.cask.cdap.data2.util.hbase.HBase96TableUtil";
  }
}
