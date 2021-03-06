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
package co.cask.cdap.examples.helloworld;

import co.cask.cdap.test.ApplicationManager;
import co.cask.cdap.test.FlowManager;
import co.cask.cdap.test.ProcedureManager;
import co.cask.cdap.test.RuntimeMetrics;
import co.cask.cdap.test.RuntimeStats;
import co.cask.cdap.test.StreamWriter;
import co.cask.cdap.test.TestBase;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test for {@link HelloWorld}.
 */
public class HelloWorldTest extends TestBase {

  @Test
  public void test() throws TimeoutException, InterruptedException, IOException {
    // Deploy the HelloWorld application
    ApplicationManager appManager = deployApplication(HelloWorld.class);

    // Start WhoFlow
    FlowManager flowManager = appManager.startFlow("WhoFlow");

    // Send stream events to the "who" Stream
    StreamWriter streamWriter = appManager.getStreamWriter("who");
    streamWriter.send("1");
    streamWriter.send("2");
    streamWriter.send("3");
    streamWriter.send("4");
    streamWriter.send("5");

    try {
      // Wait for the last Flowlet processing 5 events, or at most 5 seconds
      RuntimeMetrics metrics = RuntimeStats.getFlowletMetrics("HelloWorld", "WhoFlow", "saver");
      metrics.waitForProcessed(5, 5, TimeUnit.SECONDS);
    } finally {
      flowManager.stop();
    }

    // Start Greeting procedure and query
    ProcedureManager procedureManager = appManager.startProcedure("Greeting");
    String response = procedureManager.getClient().query("greet", ImmutableMap.<String, String>of());
    Assert.assertEquals("\"Hello 5!\"", response);

    appManager.stopAll();
  }

}
