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
package co.cask.cdap.data.stream;

import co.cask.cdap.api.flow.flowlet.StreamEvent;

/**
 * This class is a {@link StreamEvent} that also carries the corresponding {@link StreamFileOffset} that mark
 * the beginning offset of this stream event.
 */
public final class StreamEventOffset extends ForwardingStreamEvent {

  private final StreamFileOffset offset;

  public StreamEventOffset(StreamEvent event, StreamFileOffset offset) {
    super(event);
    this.offset = offset;
  }

  public StreamFileOffset getOffset() {
    return offset;
  }
}
