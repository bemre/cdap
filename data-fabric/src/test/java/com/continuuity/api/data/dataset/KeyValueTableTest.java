package com.continuuity.api.data.dataset;

import com.continuuity.api.common.Bytes;
import com.continuuity.api.data.DataSet;
import com.continuuity.api.data.OperationException;
import com.continuuity.api.data.StatusCode;
import com.continuuity.api.data.batch.Split;
import com.continuuity.api.data.batch.SplitReader;
import com.continuuity.data.dataset.DataSetTestBase;
import com.continuuity.data.operation.executor.TransactionAgent;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;

/**
 * Key value table test.
 */
public class KeyValueTableTest extends DataSetTestBase {

  static KeyValueTable kvTable;

  static final byte[] KEY1 = Bytes.toBytes("KEY1");
  static final byte[] KEY2 = Bytes.toBytes("KEY2");
  static final byte[] KEY3 = Bytes.toBytes("KEY3");
  static final byte[] VAL1 = Bytes.toBytes("VAL1");
  static final byte[] VAL2 = Bytes.toBytes("VAL2");
  static final byte[] VAL3 = Bytes.toBytes("VAL3");

  @BeforeClass
  public static void configure() throws Exception {
    DataSet kv = new KeyValueTable("test");
    DataSet t1 = new KeyValueTable("t1");
    DataSet t2 = new KeyValueTable("t2");
    DataSet tBatch = new KeyValueTable("tBatch");
    setupInstantiator(Lists.newArrayList(kv, t1, t2, tBatch));
    kvTable = instantiator.getDataSet("test");
  }

  @Test
  public void testSyncWriteReadSwapDelete() throws OperationException {

    // this test runs all operations synchronously
    TransactionAgent txAgent = newTransaction();

    // write a value and read it back
    kvTable.write(KEY1, VAL1);
    Assert.assertArrayEquals(VAL1, kvTable.read(KEY1));

    // update the value and read it back
    kvTable.write(KEY1, VAL2);
    Assert.assertArrayEquals(VAL2, kvTable.read(KEY1));

    // attempt to swap, expecting old value
    try {
      kvTable.swap(KEY1, VAL1, VAL3);
      Assert.fail("swap should have failed");
    } catch (OperationException e) {
      Assert.assertEquals(StatusCode.WRITE_CONFLICT, e.getStatus());
      Assert.assertArrayEquals(VAL2, kvTable.read(KEY1));
    }

    // swap the value and read it back
    kvTable.swap(KEY1, VAL2, VAL3);
    Assert.assertArrayEquals(VAL3, kvTable.read(KEY1));

    // delete the value and verify its gone
    kvTable.delete(KEY1);
    Assert.assertNull(kvTable.read(KEY1));
  }

  @Test
  public void testASyncWriteReadSwapDelete() throws OperationException {

    // defer all writes until commit
    TransactionAgent txAgent = newTransaction();
    // write a value
    kvTable.write(KEY2, VAL1);
    // commit the transaction
    commitTransaction(txAgent);

    // verify synchronously
    txAgent = newTransaction();
    // verify that the value is now visible
    Assert.assertArrayEquals(VAL1, kvTable.read(KEY2));
    // commit the transaction
    commitTransaction(txAgent);

    // defer all writes until commit
    txAgent = newTransaction();
    // update the value
    kvTable.write(KEY2, VAL2);
    // commit the transaction
    commitTransaction(txAgent);

    // verify synchronously
    txAgent = newTransaction();
    // verify that the value is now visible
    Assert.assertArrayEquals(VAL2, kvTable.read(KEY2));
    // commit the transaction
    commitTransaction(txAgent);

    // defer all writes until commit
    txAgent = newTransaction();
    try {
      // write a swap, this should fail
      kvTable.swap(KEY2, VAL1, VAL3);
      Assert.fail("swap should have failed");
    } catch (OperationException e) {
      Assert.assertEquals(StatusCode.WRITE_CONFLICT, e.getStatus());
      Assert.assertArrayEquals(VAL2, kvTable.read(KEY2));
    }

    // defer all writes until commit
    txAgent = newTransaction();
    // swap the value
    kvTable.swap(KEY2, VAL2, VAL3);
    // commit the transaction
    commitTransaction(txAgent);

    // verify synchronously
    txAgent = newTransaction();
    // verify the value was swapped
    Assert.assertArrayEquals(VAL3, kvTable.read(KEY2));
    // commit the transaction
    commitTransaction(txAgent);

    // defer all writes until commit
    txAgent = newTransaction();
    // delete the value
    kvTable.delete(KEY2);
    // commit the transaction
    commitTransaction(txAgent);

    // verify synchronously
    txAgent = newTransaction();
    // verify it is gone now
    Assert.assertNull(kvTable.read(KEY2));
    // commit the transaction
    commitTransaction(txAgent);
  }

  @Test
  public void testTransactionAcrossTables() throws Exception {
    KeyValueTable table1 = instantiator.getDataSet("t1");
    KeyValueTable table2 = instantiator.getDataSet("t2");

    // write a value to table1 and verify it
    TransactionAgent txAgent = newTransaction();
    table1.write(KEY1, VAL1);
    Assert.assertArrayEquals(VAL1, table1.read(KEY1));
    table2.write(KEY2, VAL2);
    Assert.assertArrayEquals(VAL2, table2.read(KEY2));
    commitTransaction(txAgent);

    // start a new transaction
    txAgent = newTransaction();
    // add a write for table 1 to the transaction
    table1.write(KEY1, VAL2);
    // submit a delete for table 2
    table2.delete(KEY2);

    try {
      // add a swap for a third table that should fail
      kvTable.swap(KEY3, VAL1, VAL1);
      Assert.fail("swap should have failed");
    } catch (OperationException e) {
      Assert.assertEquals(StatusCode.WRITE_CONFLICT, e.getStatus());
    }

    // verify synchronously that old value are still there
    table1 = instantiator.getDataSet("t1");
    table2 = instantiator.getDataSet("t2");
    txAgent = newTransaction();
    Assert.assertArrayEquals(VAL1, table1.read(KEY1));
    Assert.assertArrayEquals(VAL2, table2.read(KEY2));
    commitTransaction(txAgent);
  }

  @Test
  public void testBatchReads() throws OperationException, InterruptedException {
    KeyValueTable t = instantiator.getDataSet("tBatch");

    // start a transaction
    TransactionAgent txAgent = newTransaction();
    // write 1000 random values to the table and remember them in a set
    SortedSet<Long> keysWritten = Sets.newTreeSet();
    Random rand = new Random(451);
    for (int i = 0; i < 1000; i++) {
      long keyLong = rand.nextLong();
      byte[] key = Bytes.toBytes(keyLong);
      t.write(key, key);
      keysWritten.add(keyLong);
    }
    // commit transaction
    commitTransaction(txAgent);

    // start a sync transaction
    txAgent = newTransaction();
    // get the splits for the table
    List<Split> splits = t.getSplits();
    // read each split and verify the keys
    SortedSet<Long> keysToVerify = Sets.newTreeSet(keysWritten);
    verifySplits(t, splits, keysToVerify);

    // start a sync transaction
    txAgent = newTransaction();
    // get specific number of splits for a subrange
    keysToVerify = Sets.newTreeSet(keysWritten.subSet(0x10000000L, 0x40000000L));
    splits = t.getSplits(5, Bytes.toBytes(0x10000000L), Bytes.toBytes(0x40000000L));
    Assert.assertTrue(splits.size() <= 5);
    // read each split and verify the keys
    verifySplits(t, splits, keysToVerify);
  }

  // helper to verify that the split readers for the given splits return exactly a set of keys
  private void verifySplits(KeyValueTable t, List<Split> splits, SortedSet<Long> keysToVerify)
    throws OperationException, InterruptedException {
    // read each split and verify the keys, remove all read keys from the set
    for (Split split : splits) {
      SplitReader<byte[], byte[]> reader = t.createSplitReader(split);
      reader.initialize(split);
      while (reader.nextKeyValue()) {
        byte[] key = reader.getCurrentKey();
        byte[] value = reader.getCurrentValue();
        // verify each row has the two columns written
        Assert.assertArrayEquals(key, value);
        Assert.assertTrue(keysToVerify.remove(Bytes.toLong(key)));
      }
    }
    // verify all keys have been read
    if (!keysToVerify.isEmpty()) {
      System.out.println("Remaining [" + keysToVerify.size() + "]: " + keysToVerify);
    }
    Assert.assertTrue(keysToVerify.isEmpty());
  }

}
