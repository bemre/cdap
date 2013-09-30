package com.continuuity.data2.transaction.coprocessor;

import com.continuuity.data2.transaction.persist.TransactionSnapshot;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionServerCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionServerObserver;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.regionserver.Store;
import org.apache.hadoop.hbase.regionserver.compactions.CompactionRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * {@link org.apache.hadoop.hbase.coprocessor.RegionObserver} coprocessor that removes data from invalid transactions
 * during region compactions.
 */
public class TransactionDataJanitor extends BaseRegionObserver implements RegionServerObserver {
  private static final Log LOG = LogFactory.getLog(TransactionDataJanitor.class);

  private TransactionStateCache cache;

  /* RegionObserver implementation */

  @Override
  public void start(CoprocessorEnvironment e) throws IOException {
    this.cache = TransactionStateCache.get(e.getConfiguration());
  }

  @Override
  public void stop(CoprocessorEnvironment e) throws IOException {
    // nothing to do
  }

  @Override
  public InternalScanner preFlush(ObserverContext<RegionCoprocessorEnvironment> e, Store store,
      InternalScanner scanner) throws IOException {
    TransactionSnapshot snapshot = cache.getLatestState();
    if (snapshot != null) {
      return new DataJanitorRegionScanner(snapshot.getInvalid(), scanner);
    }
    if (LOG.isDebugEnabled()) {
      LOG.info("No current transaction state found, defaulting to normal flush scanner");
    }
    return scanner;
  }

  @Override
  public InternalScanner preCompact(ObserverContext<RegionCoprocessorEnvironment> e, Store store,
      InternalScanner scanner) throws IOException {
    TransactionSnapshot snapshot = cache.getLatestState();
    if (snapshot != null) {
      return new DataJanitorRegionScanner(cache.getLatestState().getInvalid(), scanner);
    }
    if (LOG.isDebugEnabled()) {
      LOG.info("No current transaction state found, defaulting to normal compaction scanner");
    }
    return scanner;
  }

  @Override
  public InternalScanner preCompact(ObserverContext<RegionCoprocessorEnvironment> e, Store store,
      InternalScanner scanner, CompactionRequest request) throws IOException {
    TransactionSnapshot snapshot = cache.getLatestState();
    if (snapshot != null) {
      return new DataJanitorRegionScanner(cache.getLatestState().getInvalid(), scanner);
    }
    if (LOG.isDebugEnabled()) {
      LOG.info("No current transaction state found, defaulting to normal compaction scanner");
    }
    return scanner;
  }

  /* RegionServerObserver implementation */

  @Override
  public void preStopRegionServer(ObserverContext<RegionServerCoprocessorEnvironment> env) throws IOException {
    // close the state cache
    // Note that start() is still called for the RegionServerObserver, so we still have the cache instance
    // Since the cache is read-only, we don't block region server shutdown here.
    cache.stop();
  }

  /**
   * Wraps the {@link org.apache.hadoop.hbase.regionserver.InternalScanner} instance used during compaction
   * to filter out any {@link org.apache.hadoop.hbase.KeyValue} entries associated with invalid transactions.
   */
  static class DataJanitorRegionScanner implements InternalScanner {
    private final Set<Long> invalidIds;
    private final InternalScanner internalScanner;
    private final List<KeyValue> internalResults = new ArrayList<KeyValue>();

    public DataJanitorRegionScanner(Collection<Long> invalidSet, InternalScanner scanner) {
      this.invalidIds = Sets.newHashSet(invalidSet);
      this.internalScanner = scanner;
    }

    @Override
    public boolean next(List<KeyValue> results) throws IOException {
      return next(results, -1, null);
    }

    @Override
    public boolean next(List<KeyValue> results, String metric) throws IOException {
      return next(results, -1, metric);
    }

    @Override
    public boolean next(List<KeyValue> results, int limit) throws IOException {
      return next(results, limit, null);
    }

    @Override
    public boolean next(List<KeyValue> results, int limit, String metric) throws IOException {
      internalResults.clear();

      boolean hasMore = internalScanner.next(internalResults, limit, metric);
      // TODO: due to filtering our own results may be smaller than limit, so we should retry if needed to hit it
      for (int i = 0; i < internalResults.size(); i++) {
        KeyValue kv = internalResults.get(i);
        // filter out any KeyValue with a timestamp matching an invalid write pointer
        if (!invalidIds.contains(kv.getTimestamp())) {
          results.add(kv);
        }
      }

      return hasMore;
    }

    @Override
    public void close() throws IOException {
      this.internalScanner.close();
    }
  }
}
