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

package co.cask.cdap.shell;

import co.cask.cdap.client.config.ClientConfig;
import co.cask.cdap.security.authentication.client.AuthenticationClient;
import co.cask.cdap.security.authentication.client.basic.BasicAuthenticationClient;
import co.cask.cdap.shell.command.VersionCommand;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Configuration for the CDAP CLI.
 */
public class CLIConfig {

  private static final int DEFAULT_PORT = 10000;
  private static final boolean DEFAULT_SSL = false;

  private final ClientConfig clientConfig;
  private final String version;
  private String hostname;
  private List<DestinationChangeListener> destinationChangeListeners;
  private int port;
  private int sslPort;

  /**
   * @param hostname Hostname of the CDAP server to interact with (e.g. "example.com")
   */
  public CLIConfig(String hostname) {
    this.hostname = Objects.firstNonNull(hostname, "localhost");
    this.port = DEFAULT_PORT;
    AuthenticationClient authenticationClient = new BasicAuthenticationClient();
    authenticationClient.setConnectionInfo(hostname, port, DEFAULT_SSL);
    this.clientConfig = new ClientConfig(hostname, port, authenticationClient);
    this.version = tryGetVersion();
    this.destinationChangeListeners = Lists.newArrayList();
  }

  private static String tryGetVersion() {
    try {
      InputSupplier<? extends InputStream> versionFileSupplier = new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          return VersionCommand.class.getClassLoader().getResourceAsStream("VERSION");
        }
      };
      return CharStreams.toString(CharStreams.newReaderSupplier(versionFileSupplier, Charsets.UTF_8));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public String getHost() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public int getSslPort() {
    return sslPort;
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  public String getVersion() {
    return version;
  }

  public void setDestination(URI uri) {
    boolean ssl = "https".equals(uri.getScheme());
    this.hostname = uri.getHost();
    this.port = uri.getPort();
    this.clientConfig.setHostnameAndPort(hostname, port, ssl);
    for (DestinationChangeListener listener : destinationChangeListeners) {
      listener.onDestinationChanged(hostname, port, ssl);
    }
  }

  public void addHostnameChangeListener(DestinationChangeListener listener) {
    this.destinationChangeListeners.add(listener);
  }

  /**
   * Listener for hostname changes.
   */
  public interface DestinationChangeListener {
    void onDestinationChanged(String newHostname, int newPort, boolean newSSLEnabled);
  }
}
