package com.continuuity.explore.service.hive;

import com.continuuity.explore.service.Handle;
import com.continuuity.explore.service.Status;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.hive.service.cli.OperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.continuuity.explore.service.hive.BaseHiveExploreService.OperationInfo;

/**
 * Takes care of closing operations after they are removed from the cache.
 */
public class ActiveOperationRemovalHandler implements RemovalListener<Handle, OperationInfo> {
  private static final Logger LOG = LoggerFactory.getLogger(ActiveOperationRemovalHandler.class);

  private final BaseHiveExploreService exploreService;
  private final ExecutorService executorService;

  public ActiveOperationRemovalHandler(BaseHiveExploreService exploreService, ExecutorService executorService) {
    this.exploreService = exploreService;
    this.executorService = executorService;
  }

  @Override
  public void onRemoval(RemovalNotification<Handle, OperationInfo> notification) {
    LOG.trace("Got removal notification for handle {} with cause {}", notification.getKey(), notification.getCause());
    executorService.submit(new ResourceCleanup(notification.getKey(), notification.getValue()));
  }

  private class ResourceCleanup implements Runnable {
    private final Handle handle;
    private final OperationInfo opInfo;

    private ResourceCleanup(Handle handle, OperationInfo opInfo) {
      this.handle = handle;
      this.opInfo = opInfo;
    }

    @Override
    public void run() {
      try {
        // TODO this used to be fetchStatus(operationHandle), is that a problem?
        Status status = exploreService.getStatus(handle);

        // If operation is still not complete, cancel it.
        if (status.getStatus() != Status.OpStatus.FINISHED && status.getStatus() != Status.OpStatus.CLOSED &&
          status.getStatus() != Status.OpStatus.CANCELED && status.getStatus() != Status.OpStatus.ERROR) {
          LOG.info("Cancelling handle {} with status {} due to timeout",
                   handle.getHandle(), status.getStatus());
          exploreService.cancel(handle);
        }

      } catch (Throwable e) {
        LOG.error("Could not cancel handle {} due to exception", handle.getHandle(), e);
      } finally {
        LOG.info("Timing out handle {}", handle);
        try {
          // Finally close the operation
          exploreService.closeInternal(handle, opInfo);
        } catch (Throwable e) {
          LOG.error("Exception while closing handle {}", handle, e);
        }
      }
    }
  }
}
