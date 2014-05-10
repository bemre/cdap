package com.continuuity.hive.datasets;

import com.continuuity.api.data.DataSet;
import com.continuuity.api.data.DataSetSpecification;
import com.continuuity.api.data.batch.RowScannable;
import com.continuuity.api.data.batch.Split;
import com.continuuity.api.data.batch.SplitRowScanner;
import com.continuuity.common.conf.CConfiguration;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class DatasetInputFormat implements InputFormat<Void, ObjectWritable> {
  static final String DATASET_NAME = "reactor.dataset.name";

  @Override
  public InputSplit[] getSplits(JobConf jobConf, int numSplits) throws IOException {
    RowScannable rowScannable = getDataset(jobConf.get(DATASET_NAME));

    List<Split> dsSplits = rowScannable.getSplits();
    InputSplit[] inputSplits = new InputSplit[dsSplits.size()];
    for (int i = 0; i < dsSplits.size(); i++) {
      inputSplits[i] = new DatasetInputSplit(dsSplits.get(i));
    }
    return inputSplits;
  }

  @Override
  public RecordReader<Void, ObjectWritable> getRecordReader(final InputSplit split, JobConf jobConf, Reporter reporter)
    throws IOException {
    final RowScannable rowScannable = getDataset(jobConf.get(DATASET_NAME));

    if (!(split instanceof DatasetInputSplit)) {
      throw new IOException("Invalid type for InputSplit: " + split.getClass().getName());
    }
    final DatasetInputSplit datasetInputSplit = (DatasetInputSplit) split;

    final SplitRowScanner splitRowScanner = rowScannable.createSplitScanner(
      new Split() {
        @Override
        public long getLength() {
          try {
            return split.getLength();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

    return new RecordReader<Void, ObjectWritable>() {
      private final AtomicBoolean initialized = new AtomicBoolean(false);

      private void initialize() throws IOException {
        try {
          splitRowScanner.initialize(datasetInputSplit.getDataSetSplit());
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while initializing reader", ie);
        }
        initialized.set(true);
      }

      @Override
      public boolean next(Void key, ObjectWritable value) throws IOException {
        if (!initialized.get()) {
          initialize();
        }

        try {
          boolean retVal = splitRowScanner.nextRow();
          if (retVal) {
            value.set(splitRowScanner.getCurrentRow());
          }
          return retVal;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException(e);
        }
      }

      @Override
      public Void createKey() {
        return null;
      }

      @Override
      public ObjectWritable createValue() {
        return new ObjectWritable();
      }

      @Override
      public long getPos() throws IOException {
        // Not required.
        return 0;
      }

      @Override
      public void close() throws IOException {
        splitRowScanner.close();
      }

      @Override
      public float getProgress() throws IOException {
        try {
          return splitRowScanner.getProgress();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException(e);
        }
      }
    };
  }

  static RowScannable getDataset(String datasetName)
    throws IOException {
    if (datasetName == null) {
      throw new IOException(String.format("Dataset name property %s not defined.", DATASET_NAME));
    }

    try {
      CConfiguration conf = CConfiguration.create();
      LocalDataSetUtil localDataSetUtil = new LocalDataSetUtil(conf);

      DataSetSpecification spec = localDataSetUtil.getDataSetSpecification("developer", datasetName);
      DataSet dataset = localDataSetUtil.getDataSetInstance(spec);
      if (!(dataset instanceof RowScannable)) {
        throw new IOException(
          String.format("Dataset %s does not implement RowScannable, and hence cannot be queried in Hive.",
                        datasetName));
      }
      return (RowScannable) dataset;

    } catch (Exception e) {
      throw new IOException("Exception while instantiating dataset " + datasetName, e);
    }
  }


  /**
   * This class duplicates all the functionality of
   * {@link com.continuuity.internal.app.runtime.batch.dataset.DataSetInputSplit}, but implements
   * {@link org.apache.hadoop.mapred.InputSplit} instead of {@link org.apache.hadoop.mapreduce.InputSplit}.
   */
  public static class DatasetInputSplit implements InputSplit {
    private Split dataSetSplit;

    // for Writable
    public DatasetInputSplit() {
    }

    public DatasetInputSplit(Split dataSetSplit) {
      this.dataSetSplit = dataSetSplit;
    }

    public Split getDataSetSplit() {
      return dataSetSplit;
    }

    @Override
    public long getLength() {
      return dataSetSplit.getLength();
    }

    @Override
    public String[] getLocations() throws IOException {
      // TODO: not currently exposed by BatchReadable
      return new String[0];
    }

    @Override
    public void write(DataOutput out) throws IOException {
      Text.writeString(out, dataSetSplit.getClass().getName());
      String ser = new Gson().toJson(dataSetSplit);
      Text.writeString(out, ser);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
      try {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
          classLoader = getClass().getClassLoader();
        }
        Class<? extends Split> splitClass = (Class<Split>) classLoader.loadClass(Text.readString(in));
        dataSetSplit = new Gson().fromJson(Text.readString(in), splitClass);
      } catch (ClassNotFoundException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}