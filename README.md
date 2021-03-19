# SignalFx client libraries [![Build Status](https://travis-ci.org/signalfx/signalfx-java.svg?branch=master)](https://travis-ci.org/signalfx/signalfx-java)

This repository contains libraries for instrumenting Java applications and
reporting metrics to SignalFx. You will need a SignalFx account and organization
API token to use them. For more information on SignalFx and to create an
account, go to [http://www.signalfx.com](http://www.signalfx.com).

We recommend sending metrics with Java using Codahale Metrics version 3.0+. You
can also use Yammer Metrics 2.0.x (an earlier version of Codahale Metrics). More
information on the Codahale Metrics library can be found on the
[Codahale Metrics website](https://dropwizard.github.io/metrics/).

You can also use the module `signalfx-java` to send metrics directly to SignalFx
using protocol buffers, without using Codahale or Yammer metrics.

## Supported languages

* Java 8+ with `signalfx-metrics`.

## Using this library in your project

### With Maven

If you're using Maven, add the following to your project's `pom.xml` file.

* To work with Codahale 3.0.x:

```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-codahale</artifactId>
  <version>1.0.11</version>
</dependency>
```

* To work with Yammer Metrics 2.0.x:

```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-yammer</artifactId>
  <version>1.0.11</version>
</dependency>
```

### With SBT

If you're using SBT, add the following to your project's `build.sbt` file.

* To work with Codahale 3.0.x:

```
libraryDependencies += "com.signalfx.public" % "signalfx-codahale" % "1.0.11"
```

* To work with Yammer Metrics 2.0.x:

```
libraryDependencies += "com.signalfx.public" % "signalfx-yammer" % "1.0.11"
```

### From source

You can also install this library from source by cloning the repo and using
`mvn install` as follows. However, we strongly recommend using the automated
mechanisms described above.

```
$ git clone https://github.com/signalfx/signalfx-java.git
Cloning into 'signalfx-java'...
remote: Counting objects: 930, done.
remote: Compressing objects: 100% (67/67), done.
remote: Total 930 (delta 20), reused 0 (delta 0)
Receiving objects: 100% (930/930), 146.79 KiB | 0 bytes/s, done.
Resolving deltas: 100% (289/289), done.
Checking connectivity... done.
$ cd signalfx-java
$ mvn install
[INFO] Scanning for projects...
...
...
...
[INFO] SignalFx parent .................................. SUCCESS [  2.483 s]
[INFO] SignalFx Protocol Buffer definitions ............. SUCCESS [  5.503 s]
[INFO] SignalFx Protobuf Utilities ...................... SUCCESS [  2.269 s]
[INFO] SignalFx java libraries .......................... SUCCESS [  3.728 s]
[INFO] Codahale to SignalFx ............................. SUCCESS [  2.910 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 17.120 s
[INFO] ------------------------------------------------------------------------
```

## Sending metrics

### Configuring your endpoint

Before we can send metrics to SignalFx, we need to make sure you are sending
them to the correct SignalFx realm. To determine what realm you are in, check
your profile page in the SignalFx web application (click the avatar in the upper
right and click My Profile). If you are not in the `us0` realm, you will need to
configure the `SignalFxReporter` class to send to the correct realm using one of
the following ways:

- Using the system.properties, add the `com.signalfx.api.hostname` property with
  the value of `ingest.{REALM}.signalfx.com`
- Using environment variables, set `SIGNALFX_API_HOSTNAME` to
  `ingest.{REALM}.signalfx.com`
- Manually building the `SignalFxReceiverEndpoint`, and specifying the `SignalFxReporter`
  class to use it:

```java
// Load string from properties file, env, manually, etc...
final String ingestStr = "https://ingest.{REALM}.signalfx.com";
final URL ingestUrl = new URL(ingestStr);
SignalFxReceiverEndpoint endpoint = 
    new SignalFxEndpoint(ingestUrl.getProtocol(), ingestUrl.getHost(), ingestUrl.getPort());
MetricRegistry metricRegistry = new MetricRegistry();
SignalFxReporter reporter = 
    new SignalFxReporter.Builder(metricRegistry, new StaticAuthToken(ORG_TOKEN), ingestStr)
        .setEndpoint(endpoint)
        .build();
```

### Codahale Metrics 3.0.x

#### 1. Set up the Codahale reporter

```java
final MetricRegistry metricRegistry = new MetricRegistry();
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistry,
    "ORG_TOKEN"
).build();
signalfxReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfxReporter.getMetricMetadata();
final SfxMetrics metrics = new SfxMetrics(metricRegistry, metricMetadata);
```

#### 2. Send a metric

```java
// This will send the current time in ms to SignalFx as a gauge
metrics.registerGauge("gauge", new Gauge<Long>() {
    public Long getValue() {
        return System.currentTimeMillis();
    }
});
```

#### 3. Add dimensions and metadata to metrics

```java
/*
 * This will send the size of a queue as a gauge, and attach dimension
 * 'queue_name' to the gauge.
 */
final Queue customerQueue = new ArrayBlockingQueue(100);
metrics.registerGauge("queue_size", new Gauge<Long>() {
    @Override
    public Long getValue() {
        return customerQueue.size();
    }
}, "queue_name", "customer_backlog");
```

We recommend creating your Codahale object as a field of your class (a
`Counter`, `Gauge<?>`, `Histogram` or `Timer`) then using that field to
increment or update values. If you don't want to maintain this for reasons of
code cleanliness, you can always just create it on the fly.

For example, if you wanted a timer that included a dimension indicating which
store it is from, you could use code like this.

```java
Timer t = metrics.timer("request_time", "storename", "electronics");

try (Timer.Context ignored = t.time()) {
    System.out.println("Doing store things");
}

// Or on the fly:
try (Timer.Context ignored = metrics.timer("request_time").time()) {
    // Do something
}
```

#### After setting up Codahale

After setting up a SignalFxReporter, you can use Codahale metrics as you
normally would, reported at the frequency configured by the `SignalFxReporter`.

### Yammer Metrics

You can also use this library with Yammer metrics 2.0.x as shown in the
following examples.

#### 1. Set up Yammer metrics

```java
final MetricsRegistry metricsRegistry = new MetricsRegistry();
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricsRegistery,
    "ORG_TOKEN"
).build();
signalfxReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfxReporter.getMetricMetadata();
```

Note: the `SfxMetrics` helper is not supported for Yammer metrics.

#### 2. Send a metric with Yammer metrics

```java
// This will send the current time in ms to SignalFx as a gauge
MetricName gaugeName = new MetricName("group", "type", "gauge");
Metric gauge = metricRegistry.newGauge(gaugeName, new Gauge<Long>() {
    @Override
    public Long value() {
        return System.currentTimeMillis();
    }
});
```

#### 3. Add Dimensions and SignalFx metadata to Yammer metrics

Use the MetricMetadata of the reporter as shown.

```java
final Queue customerQueue = new ArrayBlockingQueue(100);

MetricName gaugeName = new MetricName("group", "type", "gauge");
Metric gauge = metricRegistry.newGauge(gaugeName, new Gauge<Integer>() {
    @Override
    public Integer value() {
        return customerQueue.size();
    }
});

metricMetadata.forMetric(gauge)
    .withDimension("queue_name", "customer_backlog");
```

### Changing the default source

The default source name for metrics is discovered by [SourceNameHelper]
(signalfx-java/src/main/java/com/signalfx/metrics/SourceNameHelper.java).
If you want to override the default behavior, you can pass a third parameter to
your Builder and that String is then used as the source.

For example:

```
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistry,
    "ORG_TOKEN",
    "MYHOST1"
).build();
```

### Default dimensions

Sometimes there is a desire to set one or more dimension key/value pairs
on every datapoint that is reported by this library. In order to do this
call `addDimension(String key, String value)` or
`addDimensions(Map<String,String> dimensions)` on the
`SignalFxReporter.Builder` object.

Note that if `IncrementalCounter` is used to create a distributed
counter you will want to make sure that none of the dimensions passed to
`addDimension()/addDimensions()` are unique to the reporting source
(e.g. `hostname`, `AWSUniqueId`) as this will make make the counter
non-distributed. For such dimensions use
`addUniqueDimension()/addUniqueDimensions()` on the
`SignalFxReporter.Builder` object.

### AWS Integration

To enable AWS integration in SignalFx (i.e aws tag/property syncing) to a metric
you can use `com.signalfx.metrics.aws.AWSInstanceInfo`. And either add it as
a dimension in `MetricMetadata` or add it as a default dimension.

```java
String instanceInfo = AWSInstanceInfo.get()
Timer t = metrics.timer("request_time", AWSInstanceInfo.DIMENSION_NAME, instanceInfo);

/**
 * As default dimension
 */
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistry,
    "ORG_TOKEN"
).addUniqueDimension(AWSInstanceInfo.DIMENSION_NAME, instanceInfo).build();
```

### Sending metrics without using Codahale

We recommend sending metrics using Codahale as shown above. You can also
interact with our Java library directly if you do not want to use Codahale. To
do this, you will need to build the metric manually using protocol buffers as
shown in the following example. Sending both datapoints and events are now
supported using protocol buffers.

```java
SignalFxReceiverEndpoint signalFxEndpoint = new SignalFxEndpoint();
AggregateMetricSender mf = new AggregateMetricSender("test.SendMetrics",
    new HttpDataPointProtobufReceiverFactory(signalFxEndpoint).setVersion(2),
    new HttpEventProtobufReceiverFactory(signalFxEndpoint),
    new StaticAuthToken(auth_token),
    Collections.<OnSendErrorHandler> singleton(new OnSendErrorHandler() {
        @Override
        public void handleError(MetricError metricError) {
            System.out.println("Unable to POST metrics: " + metricError.getMessage());
        }
    }));

try (AggregateMetricSender.Session i = mf.createSession()) {
    i.setDatapoint(
        SignalFxProtocolBuffers.DataPoint.newBuilder()
            .setMetric("curtime")
            .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
            .setValue(
                SignalFxProtocolBuffers.Datum.newBuilder()
                    .setIntValue(System.currentTimeMillis()))
            .addDimensions(
                SignalFxProtocolBuffers.Dimension.newBuilder()
                    .setKey("source")
                    .setValue("java"))
            .build());

    i.setEvent(
            SignalFxProtocolBuffers.Event.newBuilder()
                  .setEventType("Deployments")
                  .setCategory(SignalFxProtocolBuffers.EventCategory.USER_DEFINED)
                  .setTimestamp(System.currentTimeMillis())
                  .addDimensions(
                    SignalFxProtocolBuffers.Dimension.newBuilder()
                                      .setKey("source")
                                      .setValue("java"))
                  .addProperties(
                    SignalFxProtocolBuffers.Property.newBuilder()
                                    .setKey("version")
                                    .setValue(
                                            SignalFxProtocolBuffers.PropertyValue.newBuilder()
                                                    .setIntValue(2)
                                                    .build())
                                    .build())
                  .build());
}
```

### Sending metrics through a HTTP proxy

To send metrics through a HTTP proxy one can set the standard java system
properties used to control HTTP protocol handling. There are 3 properties you
can set to specify the proxy that will be used by the HTTP protocol handler:

* `http.proxyHost`: the host name of the proxy server
* `http.proxyPort`: the port number, the default value being 80.
* `http.nonProxyHosts`: a list of hosts that should be reached directly, bypassing
  the proxy. This is a list of regular expressions separated by `|`. Any host
  matching one of these regular expressions will be reached through a direct
  connection instead of through a proxy.

Basic example:

```
$ java -Dhttp.proxyHost=webcache.mydomain.com -Dhttp.proxyPort=8080”
```

Example with directive to bypass proxy for `localhost` and `host.mydomain.com`:

```
$ java -Dhttp.proxyHost=webcache.mydomain.com -Dhttp.proxyPort=8080 -Dhttp.noProxyHosts=”localhost|host.mydomain.com”
```

### Disabling compression when sending datapoints

By default, the Java library compresses datapoint payloads when sending them to
SignalFx. This can provide significant egress volume savings when sending data
to SignalFx's ingest API. This behavior can be disabled by setting the
`com.signalfx.public.java.disableHttpCompression` system property to `true`:

```
$ java -Dcom.signalfx.public.java.disableHttpCompression=true ...
```

## Example Project

You can find a full-stack example project called "signalfx-java-examples" in
the repo.

Run it as follows:

1. Download the code and create an "auth" file in the "signalfx-java-examples"
   directory. The auth file should contain the following:

    ```
    auth=<signalfx API Token>
    host=https://ingest.signalfx.com
    ```

2. Run the following commands in your terminal to install and run the example
   project, replacing `path/to/signalfx-java-examples` with the location of the
   example project code in your environment. You must have Maven installed.

    ```
    cd path/to/signalfx-java-examples
    mvn install
    # an example for Yammer 2.x metrics
    mvn exec:java -Dexec.mainClass="com.signalfx.example.YammerExample"
    # an example for sending datapoints and events using protocol buffers
    mvn exec:java -Dexec.mainClass="com.signalfx.example.ProtobufExample"
    ```
New metrics and events from the example project should appear in SignalFx.

## Executing SignalFlow computations

SignalFlow is SignalFx's real-time analytics computation language. The
SignalFlow API allows SignalFx users to execute real-time streaming analytics
computations on the SignalFx platform. For more information, head over to our
Developers documentation:

* [SignalFlow Overview](https://developers.signalfx.com/signalflow_analytics/signalflow_overview.html)
* [SignalFlow API Reference](https://developers.signalfx.com/signalflow_reference.html)

Executing a SignalFlow program is very simple with this client library:

```java
String program = "data('cpu.utilization').mean().publish()";
SignalFlowClient flow = new SignalFlowClient("MY_TOKEN");
System.out.println("Executing " + program);
Computation computation = flow.execute(program);
for (ChannelMessage message : computation) {
    switch (message.getType()) {
    case DATA_MESSAGE:
        DataMessage dataMessage = (DataMessage) message;
        System.out.printf("%d: %s%n",
                dataMessage.getLogicalTimestampMs(), dataMessage.getData());
        break;

    case EVENT_MESSAGE:
        EventMessage eventMessage = (EventMessage) message;
        System.out.printf("%d: %s%n",
                eventMessage.getTimestampMs(),
                eventMessage.getProperties());
        break;
    }
}
```

Metadata about the timeseries is received from the iterable stream, and it
is also automatically intercepted by the client library and made available through
the ``Computation`` object returned by ``execute()``:

```java
case DATA_MESSAGE:
    DataMessage dataMessage = (DataMessage) message;
    for (Map<String, Number> datum : dataMessage.getData()) {
        Map<String,Object> metadata = computation.getMetadata(datum.getKey());
        // ...
    }
```

## License

Apache Software License v2. Copyright © 2014-2020 SignalFx
