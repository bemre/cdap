package com.continuuity.hive.client;

import com.continuuity.common.conf.Constants;
import com.continuuity.common.discovery.RandomEndpointStrategy;
import com.continuuity.common.discovery.TimeLimitEndpointStrategy;

import com.google.inject.Inject;
import org.apache.hive.beeline.BeeLine;
import org.apache.hive.jdbc.HiveDriver;
import org.apache.twill.discovery.Discoverable;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class DistributedHiveCommandExecutor implements HiveClient {
  private static final Logger LOG = LoggerFactory.getLogger(DistributedHiveCommandExecutor.class);

  public final DiscoveryServiceClient discoveryClient;

  @Inject
  public DistributedHiveCommandExecutor(DiscoveryServiceClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }

  @Override
  public void sendCommand(String cmd) throws IOException {

    LOG.info("Trying to send command: {}", cmd);
    try {
      Class.forName("org.apache.hive.jdbc.HiveDriver");
      new HiveDriver();
    } catch (Exception e) {
      LOG.error("Could not load hive driver", e);
    }

//    Discoverable hiveDiscoverable = new RandomEndpointStrategy(discoveryClient.discover(Constants.Service.HIVE)).pick();
    Discoverable hiveDiscoverable = new TimeLimitEndpointStrategy(
        new RandomEndpointStrategy(discoveryClient.discover(Constants.Service.HIVE)),
        1L, TimeUnit.SECONDS).pick();
    if (hiveDiscoverable == null) {
      LOG.error("No endpoint for service {}", Constants.Service.HIVE);
      throw new IOException("No endpoint for service " + Constants.Service.HIVE);
    }

    // The hooks are plugged at every command
    String[] args = new String[] {"-d", BeeLine.BEELINE_DEFAULT_JDBC_DRIVER,
        "-u", "'" + BeeLine.BEELINE_DEFAULT_JDBC_URL +
        hiveDiscoverable.getSocketAddress().getHostName() +
        ":" + hiveDiscoverable.getSocketAddress().getPort() +
        "/default?" +
        "hive.exec.pre.hooks=com.continuuity.hive.hooks.TransactionPreHook;" +
        "hive.exec.post.hooks=com.continuuity.hive.hooks.TransactionPostHook'",
        "-n", "hive",
        "--outputformat=table",
        "-e", cmd};

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream pout = new PrintStream(out);

    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream perr = new PrintStream(err);

    BeeLine beeLine = new BeeLine();
    beeLine.setOutputStream(pout);
    beeLine.setErrorStream(perr);
    beeLine.begin(args, null);
    beeLine.close();

    LOG.info("********* HIVE OUT = [" + out.toString("UTF-8") + "]");
    LOG.info("********* HIVE ERR = [" + err.toString("UTF-8") + "]");

    // todo should return the string result
  }
}
