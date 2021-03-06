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

package co.cask.cdap.security.tools;

import co.cask.cdap.common.utils.UsageException;
import co.cask.cdap.security.server.ExternalAuthenticationServer;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Client to get an AccessToken using username:password authentication.
 */
public class AccessTokenClient {
  static {
    // this turns off all logging but we don't need that for a cmdline tool
    Logger.getRootLogger().setLevel(Level.OFF);
  }

  /**
   * for debugging. should only be set to true in unit tests.
   * when true, program will print the stack trace after the usage.
   */
  public static boolean debug = false;

  private boolean help = false;
  // Default por numbers
  private static final int SSL_PORT = 10443;
  private static final int NO_SSL_PORT = 10000;

  private String host;
  // default will be set in parsing arguments
  private int port = NO_SSL_PORT;
  private boolean useSsl = false;
  private boolean disableCertCheck = false;

  private String username;
  private String password;
  private String filePath;
  private Options options;

  private static final class ConfigurableOptions {
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String FILE = "file";
    private static final String HELP = "help";
    private static final String SSL = "ssl";
    private static final String DISABLE_CERT_CHECK = "disable-cert-check";
  }

  /**
   * Print the usage statement and return null (or empty string if this is not
   * an error case). See getValue() for an explanation of the return type.
   *
   * @param error indicates whether this was invoked as the result of an error
   * @throws co.cask.cdap.common.utils.UsageException
   *          in case of error
   */
  void usage(boolean error) {
    PrintStream out = (error ? System.err : System.out);
    String name = "accesstoken-client";
    if (System.getProperty("script") != null) {
      name = System.getProperty("script").replaceAll("[./]", "");
    }
    out.println("Usage: ");
    out.println("  " + name + " [ --host <host> ] [ --username <username> ] [ --file <outputfile> ]");
    out.println();
    printOptions(error);
  }

  private void printOptions(boolean error) {
    PrintWriter pw = error ? new PrintWriter(System.err) : new PrintWriter(System.out);
    pw.println("Options:\n");
    HelpFormatter formatter = new HelpFormatter();
    formatter.printOptions(pw, 100, options, 0, 10);
    pw.flush();
    pw.close();
    if (error) {
      throw new UsageException();
    }
  }

  private void buildOptions() {
    options = new Options();
    options.addOption(null, ConfigurableOptions.HOST, true, "To specify the host of gateway");
    options.addOption(null, ConfigurableOptions.PORT, true, "To specify the port of gateway. " +
      String.format("Defaults to %d if router is not SSL enabled and %d if it is.", NO_SSL_PORT, SSL_PORT));
    options.addOption(null, ConfigurableOptions.USER_NAME, true, "To specify the user to login as");
    options.addOption(null, ConfigurableOptions.PASSWORD, true, "To specify the user password");
    options.addOption(null, ConfigurableOptions.FILE, true, "To specify the access token file");
    options.addOption(null, ConfigurableOptions.HELP, false, "To print this message");
    options.addOption(null, ConfigurableOptions.SSL, false, "To specify that SSL is enabled");
    options.addOption(null, ConfigurableOptions.DISABLE_CERT_CHECK, false,
                      "To specify whether to check for properly signed certificates");

  }

  /**
   * Print an error message followed by the usage statement.
   *
   * @param errorMessage the error message
   */
  void usage(String errorMessage) {
    if (errorMessage != null) {
      System.err.println("Error: " + errorMessage);
    }
    usage(true);
  }

  /**
   * Parse the command line arguments.
   */
  void parseArguments(String[] args) {
    CommandLineParser parser = new BasicParser();
    CommandLine commandLine = null;
    try {
      commandLine = parser.parse(options, args);
    } catch (org.apache.commons.cli.ParseException e) {
      System.err.println("Could not parse arguments correctly.");
      usage(true);
    }

    if (commandLine.hasOption(ConfigurableOptions.HELP)) {
      usage(false);
      help = true;
      return;
    }

    useSsl = commandLine.hasOption(ConfigurableOptions.SSL);
    disableCertCheck = commandLine.hasOption(ConfigurableOptions.DISABLE_CERT_CHECK);

    host = commandLine.getOptionValue(ConfigurableOptions.HOST, "localhost");

    if (commandLine.hasOption(ConfigurableOptions.PORT)) {
      try {
        port = Integer.parseInt(commandLine.getOptionValue(ConfigurableOptions.PORT));
      } catch (NumberFormatException e) {
        usage("--port must be an integer value");
      }
    } else {
      port = (useSsl) ? SSL_PORT : NO_SSL_PORT;
    }

    username = commandLine.getOptionValue(ConfigurableOptions.USER_NAME, System.getProperty("user.name"));

    if (username == null) {
      usage("Specify --username to login as a user.");
    }

    password = commandLine.getOptionValue(ConfigurableOptions.PASSWORD);

    if (password == null) {
      Console console = System.console();
      password = String.valueOf(console.readPassword(String.format("Password for %s: ", username)));
    }

    if (commandLine.hasOption(ConfigurableOptions.FILE)) {
      filePath = commandLine.getOptionValue(ConfigurableOptions.FILE);
    } else {
      usage("Specify --file to save to file");
    }

    if (commandLine.getArgs().length > 0) {
      usage(true);
    }
  }

  private String getAuthenticationServerAddress() throws IOException {
    HttpClient client = new DefaultHttpClient();
    String baseUrl = "http";
    // ssl settings
    if (useSsl) {
      baseUrl = "https";
      if (disableCertCheck) {
        try {
          client = getHTTPClient();
        } catch (Exception e) {
          errorDebugExit("Could not create HTTP Client with SSL enabled", e);
          System.exit(1);
        }
      }
    }
    HttpGet get = new HttpGet(String.format("%s://%s:%d", baseUrl, host, port));
    HttpResponse response = client.execute(get);

    if (response.getStatusLine().getStatusCode() == 200) {
      System.out.println("Security is not enabled. No Access Token may be acquired");
      System.exit(0);
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteStreams.copy(response.getEntity().getContent(), bos);
    String responseBody = bos.toString("UTF-8");
    bos.close();
    JsonParser parser = new JsonParser();
    JsonObject responseJson = (JsonObject) parser.parse(responseBody);
    JsonArray addresses = responseJson.get("auth_uri").getAsJsonArray();
    ArrayList<String> list = new ArrayList<String>();
    for (JsonElement e : addresses) {
      list.add(e.getAsString());
    }
    return list.get(new Random().nextInt(list.size()));
  }

  protected DefaultHttpClient getHTTPClient() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("SSL");

    // set up a TrustManager that trusts everything
    sslContext.init(null, new TrustManager[] { new X509TrustManager() {
      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
        //
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s)
        throws CertificateException {
        //
      }

    } }, new SecureRandom());

    SSLSocketFactory sf = new SSLSocketFactory(sslContext);
    Scheme httpsScheme = new Scheme("https", 10101, sf);
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(httpsScheme);

    // apache HttpClient version >4.2 should use BasicClientConnectionManager
    ClientConnectionManager cm = new BasicClientConnectionManager(schemeRegistry);
    return new DefaultHttpClient(cm);
  }

  public String execute0(String[] args) {
    buildOptions();
    parseArguments(args);
    if (help) {
      return "";
    }

    String baseUrl;
    try {
      baseUrl = getAuthenticationServerAddress();
    } catch (IOException e) {
      errorDebugExit("Could not find Authentication service to connect to.", e);
      return null;
    }

    System.out.println(String.format("Authentication server address is: %s", baseUrl));
    System.out.println(String.format("Authenticating as: %s", username));

    HttpClient client = new DefaultHttpClient();
    if (useSsl && disableCertCheck) {
      try {
        client = getHTTPClient();
      } catch (Exception e) {
        errorDebugExit("Could not create HTTP Client with SSL enabled", e);
        return null;
      }
    }

    // construct the full URL and verify its well-formedness
    try {
      URI.create(baseUrl);
    } catch (IllegalArgumentException e) {
      System.err.println("Invalid base URL '" + baseUrl + "'. Check the validity of --host or --port arguments.");
      return null;
    }

    HttpGet get = new HttpGet(baseUrl);
    String auth = Base64.encodeBase64String(String.format("%s:%s", username, password).getBytes());
    auth = auth.replaceAll("(\r|\n)", "");
    get.addHeader("Authorization", String.format("Basic %s", auth));
    HttpResponse response;
    try {
      response = client.execute(get);
    } catch (IOException e) {
      errorDebugExit("Error sending HTTP request: " + e.getMessage(), e);
      return null;
    }
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      System.out.println("Authentication failed. Please ensure that the username and password provided are correct.");
      return null;
    } else {
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteStreams.copy(response.getEntity().getContent(), bos);
        String responseBody = bos.toString("UTF-8");
        bos.close();
        JsonParser parser = new JsonParser();
        JsonObject responseJson = (JsonObject) parser.parse(responseBody);
        String token = responseJson.get(ExternalAuthenticationServer.ResponseFields.ACCESS_TOKEN).getAsString();

        PrintWriter writer = new PrintWriter(filePath, "UTF-8");
        writer.write(token);
        writer.close();
        System.out.println("Your Access Token is:" + token);
        System.out.println("Access Token saved to file " + filePath);
      } catch (Exception e) {
        System.err.println("Could not parse response contents.");
        e.printStackTrace(System.err);
        return null;
      }
    }
    client.getConnectionManager().shutdown();
    return "OK.";
  }

  /**
   * Prints the message, if debug is enabled, prints the error message stacktrace, then exits
   * @param message
   * @param e
   */
  private void errorDebugExit(String message, Exception e) {
    System.err.println(message);
    if (debug) {
      e.printStackTrace();
    }
    System.exit(1);
  }

  public String execute(String[] args) {
    try {
      return execute0(args);
    } catch (UsageException e) {
      if (debug) {
        System.err.println("Exception for arguments: " + Arrays.toString(args) + ". Exception: " + e);
        e.printStackTrace(System.err);
      }
    }
    return null;
  }

  public static void main(String[] args) {
    AccessTokenClient accessTokenClient = new AccessTokenClient();
    String value = accessTokenClient.execute(args);
    if (value == null) {
      System.exit(1);
    }
  }
}
