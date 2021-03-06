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
package co.cask.cdap.internal.app.runtime.batch;

import co.cask.cdap.api.mapreduce.MapReduceContext;
import co.cask.cdap.internal.app.runtime.AbstractProgramController;
import com.google.common.util.concurrent.Service;
import org.apache.twill.common.ServiceListenerAdapter;
import org.apache.twill.common.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ProgramController for MapReduce. It mainly is an adapter for reflecting the state changes in
 * {@link MapReduceRuntimeService}.
 */
public final class MapReduceProgramController extends AbstractProgramController {

  private static final Logger LOG = LoggerFactory.getLogger(MapReduceProgramController.class);

  private final Service mapReduceRuntimeService;
  private final MapReduceContext context;

  MapReduceProgramController(Service mapReduceRuntimeService, BasicMapReduceContext context) {
    super(context.getProgramName(), context.getRunId());
    this.mapReduceRuntimeService = mapReduceRuntimeService;
    this.context = context;
    listenToRuntimeState(mapReduceRuntimeService);
  }

  @Override
  protected void doSuspend() throws Exception {
    // No-op
  }

  @Override
  protected void doResume() throws Exception {
    // No-op
  }

  @Override
  protected void doStop() throws Exception {
    if (mapReduceRuntimeService.state() != Service.State.TERMINATED
      && mapReduceRuntimeService.state() != Service.State.FAILED) {
      mapReduceRuntimeService.stopAndWait();
    }
  }

  @Override
  protected void doCommand(String name, Object value) throws Exception {
    // No-op
  }

  /**
   * Returns the {@link MapReduceContext} for MapReduce run represented by this controller.
   */
  public MapReduceContext getContext() {
    return context;
  }

  private void listenToRuntimeState(Service service) {
    service.addListener(new ServiceListenerAdapter() {
      @Override
      public void running() {
        started();
      }

      @Override
      public void failed(Service.State from, Throwable failure) {
        LOG.error("MapReduce terminated with exception", failure);
        error(failure);
      }

      @Override
      public void terminated(Service.State from) {
        if (getState() != State.STOPPING) {
          // MapReduce completed by itself. Simply signal the state change of this controller.
          stop();
        }
      }
    }, Threads.SAME_THREAD_EXECUTOR);
  }
}
