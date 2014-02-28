## AccessLogApp Example ##

##### A Continuuity Reactor Application demonstrating Streams, Flows, DataSets and Procedures #####

### Overview###

This example demonstrates a simple application for real-time streaming log analysis—computing the number of occurrences of each HTTP status code by processing real-time Apache access log data.

The example introduces the basic constructs of the Continuuity Reactor programming paradigm: Applications, Streams, Flows, Procedures and DataSets.

In any real-time application, there are four distinct areas:

1. Data Collection: how to get the relevant signals needed by the real-time application for processing
2. Data Processing: the application logic to process the signals into meaningful analysis
3. Data Storage: saving the computed analysis in appropriate data structures
4. Data Queries: serving the computed data to any down-stream application that wants to use the data

The `AccessLogApp` application demonstrates using the abstractions of the Continuuity Reactor to cover these four areas.

Let's look at each one in turn.

##### The AccessLogApp application #####

All of the components (Streams, Flows, DataSets, and Procedures) of the application are tied together as a deployable entity by the class `AccessLogApp`, an implementation of `com.continuuity.api.Application`.

		public class AccessLogApp implements Application {
  		  // The constant to define the row key of a table
 		  private static final byte [] ROW_KEY = Bytes.toBytes("status");

  		  @Override
 		  public ApplicationSpecification configure() {
   			return ApplicationSpecification.Builder.with()
      		  .setName("AccessLogAnalytics")
     		  .setDescription("Apache log event analysis app")
     	      // Ingest data into the app via Streams
      		  .withStreams()
        		.add(new Stream("logEventStream"))
      		  // Store processed data in Datasets
      		  .withDataSets()
        		.add(new Table("statusCodesTable"))
      		  // Process log events in real-time using Flows
      		  .withFlows()
        		.add(new LogAnalyticsFlow())
      		  // Query the processed data using Procedures
      		  .withProcedures()
        		.add(new StatusCodeProcedure())
      		  .noMapReduce()
      		  .noWorkflow()
      		.build();
  		}
  		
  		
##### Streams for data collection #####

Streams are the primary means for bringing data from external systems into the Continuuity Reactor in real-time.

Data can be written to streams using REST. In this example, a Stream named *logEventStream* is used to ingest Apache access logs.

The Stream is configured to ingest data using the Apache Common Log Format. Here is a sample event from a log:

	165.225.156.91 - - [09/Jan/2014:21:28:53 -0400] "GET /index.html HTTP/1.1" 200 225 "http://continuuity.com" "Mozilla/4.08 [en] (Win98; I ;Nav)"

If data is unavailable, a hyphen ("-") is used. Reading from left to right, this format contains:

- the source IP address
- the client's identity
- the remote userid (if using HTTP authentication)
- the date, time, and time zone of the request
- the actual content of the request
- the server's status code to the request
- the size of the data block returned to the client, in bytes

If the log is in Combined Log Format (as is the above example), two additional fields will be present:

- the referrer HTTP request header
- the user-agent HTTP request header

##### Flows and Flowlets for real-time data processing #####

Data ingested through Streams can be processed in real-time using Flows, which are user-implemented realtime-stream processors.

A Flow is comprised of one or more Flowlets that are wired together as a Directed Acyclic Graph (DAG). Each Flowlet is able to perform custom logic and execute data operations for each individual data object processed.

In the example, two Flowlets are used to process the data:

- *parser*: parses the Apache access log entries coming into the *logEventStream* Stream. It is implemented by `LogEventParseFlowlet`.
- *counter*: aggregates the HTTP status codes from the Apache access log entries. It is implemented by `LogCountFlowlet`.

The *parser* and *counter* Flowlets are wired together by the Flow implementation class `LogAnalyticsFlow`.

##### DataSets for data storage#####

The processed data is stored in a Table DataSet named *statuscodes*. The computed analysis—a count of each HTTP status code—is stored in a row named *status*, with the HTTP status code as the column and the count as the column value.

##### Procedures for real-time queries #####

The data in DataSets can be served using Procedures for any real-time querying of the aggregated results. The `AccessLogApp` example has a procedure to retrieve all status codes and counts.

### Building and running the App and example ###

In this remainder of this document, we refer to the Continuuity Reactor runtime as "application", and the example code that is running on it as an "app".

In this example, you can either build the app from source or deploy the already-compiled JAR file. In either case, you then start a Continuuity Reactor, deploy the app, and then run the example by injecting Apache access log entries from an example file into the app.

As you do so, you can query the app to see the results of its processing the log entries.

When finished, stop the app as described below.

##### Building the AccessLogApp####

From the project root, build AccessLogApp with the following [Apache Maven](http://maven.apache.org/) command: 

	$ mvn clean package

##### Deploying and starting the App #####

Make sure an instance of the Continuuity Reactor is running and available. From within the SDK root directory, this command will start Reactor in local mode:

	$ bin/continuuity-reactor start

From within the Reactor Dashboard ([http://localhost:9999/](http://localhost:9999/) in local mode):

1. Drag and drop the App JAR file (`target/logger1-1.0-SNAPSHOT.jar`) onto your browser window.

	Alternatively, use the *Load App* button found on the *Overview* of the Reactor Dashboard.
	
2. Once loaded, select `AccessLogAnalytics` app from the list. 

	On the app's detail page, click the *Start* button on **both** the *Process* and *Query* lists.
	
Command line tools are also available to deploy and manage apps. From within the project root:

1. To deploy the App JAR file, run 

		$ bin/deploy --app target/logger-1.0-SNAPSHOT.jar [--gateway <hostname>]
		
2. To start the App, run 

		$ bin/log-analytics --action start [--gateway <hostname>]

##### Running the example ####

###### Injecting Apache access log entries into the App ######

Running this script will inject Apache access log entries from the log file `resources/apache.accesslog` to a Stream named *logEventStream* in the `AccessLogApp`:

	$ bin/inject-data [--gateway <hostname>]

###### Query ######

There are two ways to query the *statuscodes* DataSet:

1. Send a query via an HTTP request using the `curl` command. For example:

		curl -X POST 'http://localhost:10000/v2/apps/AccessLogAnalytics/procedures/StatusCodeProcedure/methods/getCounts'

2. Type a procedure method name, in this case `getCounts`, in the Query page of the Reactor Dashboard:

	In the Continuuity Reactor Dashboard:
	- Click the *Query* button.
	- Click on the *StatusCodeProcedure* procedure.
	- Type `get-counts` in the *Method* text box.
	- Click the *Execute* button.
	- The results of the occurrences for each HTTP status code are displayed in the dashboard in JSON format. For example:
	
			{"200":21, "301":1,"404":19} 
			
##### Stopping the App #####
	
Either:

- On the App detail page of the Reactor Dashboard, click the *Stop* button on **both** the *Process* and *Query* lists; or
- Run 

		$ bin/log-analytics --action stop [--gateway <hostname>]

Copyright © 2014 Continuuity, Inc.

Continuuity and Continuuity Reactor are trademarks of Continuuity, Inc. All rights reserved.
