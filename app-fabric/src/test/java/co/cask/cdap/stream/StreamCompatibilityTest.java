/*
 * Copyright 2014 Cask, Inc.
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
package co.cask.cdap.stream;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.DatasetDefinition;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.api.stream.StreamEventData;
import co.cask.cdap.app.program.Program;
import co.cask.cdap.app.runtime.ProgramController;
import co.cask.cdap.app.runtime.ProgramRunner;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.io.BinaryEncoder;
import co.cask.cdap.common.io.Encoder;
import co.cask.cdap.common.queue.QueueName;
import co.cask.cdap.common.stream.DefaultStreamEvent;
import co.cask.cdap.common.stream.StreamEventCodec;
import co.cask.cdap.common.stream.StreamEventDataCodec;
import co.cask.cdap.data.dataset.DataSetInstantiator;
import co.cask.cdap.data.operation.StatusCode;
import co.cask.cdap.data2.OperationException;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.data2.queue.QueueClientFactory;
import co.cask.cdap.data2.queue.QueueEntry;
import co.cask.cdap.data2.queue.QueueProducer;
import co.cask.cdap.internal.app.deploy.pipeline.ApplicationWithPrograms;
import co.cask.cdap.internal.app.runtime.ProgramRunnerFactory;
import co.cask.cdap.internal.app.runtime.SimpleProgramOptions;
import co.cask.cdap.internal.io.Schema;
import co.cask.cdap.internal.io.SchemaGenerator;
import co.cask.cdap.stream.app.StreamApp;
import co.cask.cdap.test.internal.AppFabricTestHelper;
import com.continuuity.tephra.Transaction;
import com.continuuity.tephra.TransactionAware;
import com.continuuity.tephra.TransactionExecutor;
import com.continuuity.tephra.TransactionExecutorFactory;
import com.continuuity.tephra.TransactionFailureException;
import com.continuuity.tephra.TransactionSystemClient;
import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 *
 */
public class StreamCompatibilityTest {

  @ClassRule
  public static TemporaryFolder tmpFolder = new TemporaryFolder();

  public static final Supplier<File> TEMP_DIR_SUPPLIER = new Supplier<File>() {

    @Override
    public File get() {
      try {
        return tmpFolder.newFolder();
      } catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  };

  @Test
  public void decodeOldStream() throws Exception {
    Injector injector = AppFabricTestHelper.getInjector().createChildInjector(new StreamWriterModule());
    StreamWriter writer = injector.getInstance(StreamWriterFactory.class).create(QueueName.fromStream("stream"));
    StreamEventEncoder oldEncoder = injector.getInstance(Key.get(StreamEventEncoder.class, Names.named("old")));
    StreamEventEncoder newEncoder = injector.getInstance(Key.get(StreamEventEncoder.class, Names.named("new")));

    final ApplicationWithPrograms app = AppFabricTestHelper.deployApplicationWithManager(StreamApp.class,
                                                                                         TEMP_DIR_SUPPLIER);
    ProgramRunnerFactory runnerFactory = injector.getInstance(ProgramRunnerFactory.class);

    ProgramRunner flowRunner = runnerFactory.create(ProgramRunnerFactory.Type.FLOW);
    Program flowProgram = app.getPrograms().iterator().next();
    ProgramController controller = flowRunner.run(flowProgram, new SimpleProgramOptions(flowProgram));

    // Writer to stream with old stream format
    writer.write("Old stream event", oldEncoder);
    writer.write("New stream event", newEncoder);

    // Read the data from dataset
    DatasetFramework datasetFramework = AppFabricTestHelper.getInjector().getInstance(DatasetFramework.class);

    DataSetInstantiator dataSetInstantiator =
      new DataSetInstantiator(datasetFramework, CConfiguration.create(),
                              getClass().getClassLoader());

    final KeyValueTable streamOut = dataSetInstantiator.getDataSet("streamout", DatasetDefinition.NO_ARGUMENTS, null);
    TransactionExecutorFactory txExecutorFactory =
      AppFabricTestHelper.getInjector().getInstance(TransactionExecutorFactory.class);

    // Should be able to read by old and new stream event
    int trial = 0;
    while (trial < 60) {
      try {
        txExecutorFactory.createExecutor(dataSetInstantiator.getTransactionAware())
          .execute(new TransactionExecutor.Subroutine() {
            @Override
            public void apply() throws Exception {
              Assert.assertEquals(1L, Bytes.toLong(streamOut.read("Old stream event".getBytes(Charsets.UTF_8))));
              Assert.assertEquals(1L, Bytes.toLong(streamOut.read("New stream event".getBytes(Charsets.UTF_8))));
            }
          });
        break;
      } catch (TransactionFailureException e) {
        // No-op
        trial++;
        TimeUnit.SECONDS.sleep(1);
      }
    }
    Assert.assertTrue(trial < 60);

    controller.stop().get();
  }

  /**
   * Encode StreamEvent to byte[] to write to Stream.
   */
  private interface StreamEventEncoder {
    byte[] encode(StreamEvent event) throws IOException;
  }

  /**
   * Encode StreamEvent as old schema.
   */
  private static final class OldStreamEventEncoder implements StreamEventEncoder {

    private final Schema schema;

    @Inject
    OldStreamEventEncoder(SchemaGenerator schemaGenerator) throws Exception {
      schema = schemaGenerator.generate(StreamEventData.class);
    }

    @Override
    public byte[] encode(StreamEvent event) throws IOException {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      Encoder encoder = new BinaryEncoder(os);

      // Write the schema hash
      os.write(schema.getSchemaHash().toByteArray());
      // Write the data
      StreamEventDataCodec.encode(event, encoder);

      return os.toByteArray();
    }
  }

  /**
   * Encode StreamEvent with new schema.
   */
  private static final class NewStreamEventEncoder implements StreamEventEncoder {

    private final StreamEventCodec codec = new StreamEventCodec();

    @Override
    public byte[] encode(StreamEvent event) throws IOException {
      return codec.encodePayload(event);
    }
  }

  /**
   * Factory for creating StreamWriter
   */
  private interface StreamWriterFactory {
    StreamWriter create(QueueName queueName);
  }

  private static final class StreamWriter {

    private final QueueProducer producer;
    private final TransactionSystemClient txSystemClient;

    @Inject
    public StreamWriter(QueueClientFactory queueClientFactory,
                        TransactionSystemClient txSystemClient,
                        @Assisted QueueName queueName) throws Exception {
      this.producer = queueClientFactory.createProducer(queueName);
      this.txSystemClient = txSystemClient;
    }

    public void write(String data, StreamEventEncoder encoder) throws Exception {
      StreamEvent event = new DefaultStreamEvent(ImmutableMap.<String, String>of(), Charsets.UTF_8.encode(data));
      enqueue(encoder.encode(event));
    }

    private void enqueue(byte[] bytes) throws IOException {
      TransactionAware txAware = (TransactionAware) producer;

      // start tx to write in queue in tx
      Transaction tx = txSystemClient.startShort();
      txAware.startTx(tx);
      try {
        producer.enqueue(new QueueEntry(bytes));
        if (!txSystemClient.canCommit(tx, txAware.getTxChanges())
          || !txAware.commitTx()
          || !txSystemClient.commit(tx)) {
          throw new OperationException(StatusCode.TRANSACTION_CONFLICT, "Fail to commit");
        }
        txAware.postTxCommit();
      } catch (Exception e) {
        try {
          txAware.rollbackTx();
          txSystemClient.abort(tx);
        } catch (Exception ex) {
          throw new IOException(ex);
        }
        throw new IOException(e);
      }
    }
  }

  private static final class StreamWriterModule extends AbstractModule {

    @Override
    protected void configure() {
      install(
        new FactoryModuleBuilder()
          .build(StreamWriterFactory.class)
      );
      bind(StreamEventEncoder.class).annotatedWith(Names.named("old")).to(OldStreamEventEncoder.class);
      bind(StreamEventEncoder.class).annotatedWith(Names.named("new")).to(NewStreamEventEncoder.class);
    }
  }

}
