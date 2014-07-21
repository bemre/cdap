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
package com.continuuity.api.stream;

import java.nio.ByteBuffer;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Represents data in one stream event.
 */
@Nonnull
public interface StreamEventData {

  /**
   * @return A {@link java.nio.ByteBuffer} that is the payload of the event.
   */
  ByteBuffer getBody();

  /**
   * @return An immutable map of all headers included in this event.
   */
  Map<String, String> getHeaders();
}