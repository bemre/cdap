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

package co.cask.cdap.data2.transaction.stream;

import co.cask.cdap.common.queue.QueueName;
import co.cask.cdap.data2.transaction.EntityAdmin;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public interface StreamAdmin extends EntityAdmin {

  /**
   * Deletes all entries for all queues.
   */
  void dropAll() throws Exception;

  /**
   * Sets the number of consumer instances for the given consumer group in a queue.
   * @param streamName Name of the stream.
   * @param groupId The consumer group to alter.
   * @param instances Number of instances.
   */
  void configureInstances(QueueName streamName, long groupId, int instances) throws Exception;


  /**
   * Sets the consumer groups information for the given queue.
   * @param streamName Name of the stream.
   * @param groupInfo A map from groupId to number of instances of each group.
   */
  void configureGroups(QueueName streamName, Map<Long, Integer> groupInfo) throws Exception;

  /**
   * Performs upgrade action for all streams.
   */
  void upgrade() throws Exception;

  /**
   * Returns the configuration of the given stream.
   * @param streamName Name of the stream.
   * @return A {@link StreamConfig} instance.
   * @throws IOException If the stream doesn't exists.
   */
  StreamConfig getConfig(String streamName) throws IOException;

  /**
   * Overwrites existing configuration for the given stream.
   * @param config New configuration of the stream.
   */
  void updateConfig(StreamConfig config) throws IOException;
}
