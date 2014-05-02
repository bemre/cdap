/*
 * Copyright 2014 Continuuity,Inc. All Rights Reserved.
 */
package com.continuuity.data2.transaction.stream.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.data.DataSetAccessor;
import com.continuuity.data.file.FileReader;
import com.continuuity.data.stream.StreamEventOffset;
import com.continuuity.data.stream.StreamFileOffset;
import com.continuuity.data.stream.StreamFileType;
import com.continuuity.data.stream.StreamUtils;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.transaction.queue.QueueConstants;
import com.continuuity.data2.transaction.queue.QueueEntryRow;
import com.continuuity.data2.transaction.queue.hbase.HBaseQueueAdmin;
import com.continuuity.data2.transaction.stream.AbstractStreamFileConsumerFactory;
import com.continuuity.data2.transaction.stream.StreamAdmin;
import com.continuuity.data2.transaction.stream.StreamConfig;
import com.continuuity.data2.transaction.stream.StreamConsumer;
import com.continuuity.data2.transaction.stream.StreamConsumerStateStore;
import com.continuuity.data2.transaction.stream.StreamConsumerStateStoreFactory;
import com.continuuity.data2.util.hbase.HBaseTableUtil;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.twill.filesystem.Location;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.continuuity.data2.transaction.stream.StreamConsumerFactory} that reads from stream file
 * and uses HBase as the store for consumer process states.
 */
public final class HBaseStreamFileConsumerFactory extends AbstractStreamFileConsumerFactory {

  private final HBaseTableUtil tableUtil;
  private final CConfiguration cConf;
  private final Configuration hConf;
  private HBaseAdmin admin;

  @Inject
  HBaseStreamFileConsumerFactory(DataSetAccessor dataSetAccessor, StreamAdmin streamAdmin,
                                 StreamConsumerStateStoreFactory stateStoreFactory,
                                 CConfiguration cConf, Configuration hConf, HBaseTableUtil tableUtil) {
    super(dataSetAccessor, streamAdmin, stateStoreFactory);
    this.hConf = hConf;
    this.cConf = cConf;
    this.tableUtil = tableUtil;
  }

  @Override
  protected StreamConsumer create(String tableName, StreamConfig streamConfig, ConsumerConfig consumerConfig,
                                  StreamConsumerStateStore stateStore,
                                  FileReader<StreamEventOffset, Iterable<StreamFileOffset>> reader) throws IOException {

    String hBaseTableName = HBaseTableUtil.getHBaseTableName(tableName);
    HTableDescriptor htd = new HTableDescriptor(hBaseTableName);

    HColumnDescriptor hcd = new HColumnDescriptor(QueueEntryRow.COLUMN_FAMILY);
    htd.addFamily(hcd);
    hcd.setMaxVersions(1);

    int splits = cConf.getInt(Constants.Stream.CONSUMER_TABLE_PRESPLITS, 1);
    byte[][] splitKeys = HBaseTableUtil.getSplitKeys(splits);

    tableUtil.createTableIfNotExists(getAdmin(), Bytes.toBytes(hBaseTableName), htd, splitKeys,
                                     QueueConstants.MAX_CREATE_TABLE_WAIT, TimeUnit.MILLISECONDS);

    HTable hTable = new HTable(hConf, hBaseTableName);
    return new HBaseStreamFileConsumer(streamConfig, consumerConfig,
                                       hTable, reader, stateStore, HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR);
  }

  @Override
  protected void getFileOffsets(Location partitionLocation,
                                Collection<? super StreamFileOffset> fileOffsets) throws IOException {
    // TODO: Support dynamic writer instances discovery
    // Current assume it won't change and is based on cConf
    int instances = cConf.getInt(Constants.Stream.CONTAINER_INSTANCES, 0);
    String filePrefix = cConf.get(Constants.Stream.FILE_PREFIX);
    for (int i = 0; i < instances; i++) {
      // The actual file prefix in distributed mode is formed by file prefix in cConf + writer instance id
      String streamFilePrefix = filePrefix + '.' + i;
      Location eventLocation = StreamUtils.createStreamLocation(partitionLocation, streamFilePrefix,
                                                                0, StreamFileType.EVENT);
      fileOffsets.add(new StreamFileOffset(eventLocation, 0));
    }
  }

  private synchronized HBaseAdmin getAdmin() throws IOException {
    if (admin == null) {
      admin = new HBaseAdmin(hConf);
    }
    return admin;
  }
}