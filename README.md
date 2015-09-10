# SignalFx client libraries [![Build Status](https://travis-ci.org/signalfx/signalfx-java.svg?branch=master)](https://travis-ci.org/signalfx/signalfx-java)

This repository contains libraries for instrumenting applications in a
variety of languages and reporting these metrics to SignalFx. You will
need a SignalFx account and organization API token to use those. For
more information on SignalFx and to create an account, go to
http://www.signalfx.com.

The recommended way to send metrics with java is to use codahale metrics.
If you want to send metrics without integrating with codahale, the module
signalfx-java will support this.

## Supported languages

* Java 6+ with `signalfx-metrics`.

## Installation

### With Maven

* Codahale 3.0.x
```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-codahale</artifactId>
  <version>0.0.21</version>
</dependency>
```

* Yammer Metrics 2.0.x
```xml
<dependency>
<groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-yammer</artifactId>
  <version>0.0.23</version>
</dependency>
```

### From source (Not recommended)

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

### Codahale

Codahale metrics are the recommended way of integrating your java code with SignalFx.
More information on the Codahale Metrics library can be found on the
[Codahale Metrics website](https://dropwizard.github.io/metrics/).

#### Setting up Codahale

```java
final MetricRegistry metricRegistery = new MetricRegistry();
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistery,
    "SIGNALFX_AUTH_TOKEN"
).build();
signalfxReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfxReporter.getMetricMetadata();
```

#### Sending a metric with Codahale

* Codahale 3.0.x
```java
        // This will send the current time in ms to signalfx as a gauge

        metricRegistery.register("gauge", new Gauge<Long>() {
            public Long getValue() {
                return System.currentTimeMillis();
            }
        });
```

* Yammer Metrics 2.0.x
```java
        // This will send the current time in ms to signalfx as a gauge

        MetricName gaugeName = new MetricName("group", "type", "gauge");
        Metric gauge = metricRegistery.newGauge(gaugeName, new Gauge<Long>() {
            @Override
            public Long value() {
                return System.currentTimeMillis();
            }
        });
```

#### Adding Dimensions and SignalFx metadata to Codahale metrics

* Codahale 3.0.x

You can add SignalFx specific metadata to codahale metrics by using
the MetricMetadata of the reporter.  When you use MetricMetadata, rather
than register your metric directly with the metricRegistry, you should
call the .register() method you get from the call forMetric().  This will
construct a unique codahale string for your metric.

```java
        // This will send the size of a queue as a gauge, tagging the queue
        // with a dimension to describe it
        
        final Queue customerQueue = new ArrayBlockingQueue(100);
        metricMetadata.forMetric(new Gauge<Long>() {
            @Override
            public Long getValue() {
                return customerQueue.size();
            }
        }).withDimension("queue_name", "customer_backlog")
                .register(metricRegistery);
```

* Yammer Metrics 2.0.x

You can add SignalFx specific metadata to yammer metrics by using
the MetricMetadata of the reporter.
```java
        final Queue customerQueue = new ArrayBlockingQueue(100);

        MetricName gaugeName = new MetricName("group", "type", "gauge");
        Metric gauge = metricRegistery.newGauge(gaugeName, new Gauge<Integer>() {
            @Override
            public Integer value() {
                return customerQueue.size();
            }
        });

        metricMetadata.forMetric(gauge).withDimension("queue_name", "customer_backlog");
```

#### Adding Dimensions without knowing if they already exist

* Codahale 3.0.x

It is recommended to create your Codahale object as a counter
or gauge as a field of your class then use that field to increment
values, but if you don't want to maintain this for code cleanliness
you can create it on the fly with our builders.  For example, if you
wanted a timer with the dimension of the store it is from you could
use code like this.

```java
        Timer t = metricMetadata.forBuilder(MetricBuilder.TIMERS)
                .withMetricName("request_time")
                .withDimension("storename", "electronics")
                .createOrGet(metricRegistery);

        Timer.Context c = t.time();
        try {
            System.out.println("Doing store things");
        } finally {
            c.close();
        }

        // Java 7 alternative:
//        try (Timer.Context ignored = t.time()) {
//            System.out.println("Doing store things");
//        }

```

* Yammer Metrics 2.0.x

NOT SUPPORTED;


#### Changing the default source

The default source name for metrics is discovered by [SourceNameHelper]( signalfx-java/src/main/java/com/signalfx/metrics/SourceNameHelper.java).  If you want to override the default behavior, you can pass a third parameter to your Builder and that String is then used as the source.  If you are using AWS, we provide a helper to extract your AWS instance ID and use that as the source.  For example:

```
final SignalFxReporter signalfxReporter = new SignalFxReporter.Builder(
    metricRegistery,
    "SIGNALFX_AUTH_TOKEN",
    SourceNameHelper.getAwsInstanceId()
).build();
```

#### After setting up Codahale

After setting up a SignalFxReporter, you can use codahale metrics as
you normally would, reported at the frequency configured to the
`SignalFxReporter`.

## Example Project

You can find full-stack example project called "signalfx-yammer-example" in the repo.
To run it do the following steps...

* 1. create "auth" file in the "signalfx-yammer-example" which containes:

```
    auth=<signalfx API Token>
    host=https://ingest.signalfx.com
```
* 2. from terminal run:

```
    cd <signalfx-yammer-example path>
    mvn install
    mvn exec:java -Dexec.mainClass="com.signalfx.yammer.example.App"
```

make sure you have Maven installed

* 3. Go to signalfx.com and verify you are getting data

## Sending metrics without using codahale (not recommended)

You can also interact with our java library directly if you do not want to use
 Codahale.  To do this, you will need to build the metric to send to
 signalfx manually using protocol buffers.
```java
        DataPointReceiverEndpoint dataPointEndpoint = new DataPointEndpoint();
        AggregateMetricSender mf =
                new AggregateMetricSender("test.SendMetrics",
                                          new HttpDataPointProtobufReceiverFactory(
                                                  dataPointEndpoint)
                                                  .setVersion(2),
                                          new StaticAuthToken(auth_token),
                                          Collections.<OnSendErrorHandler>singleton(new OnSendErrorHandler() {
                                            @Override
                                            public void handleError(MetricError metricError) {
                                              System.out.println("Unable to POST metrics: " + metricError.getMessage());
                                            }
                                          }));

                                          Collections.<OnSendErrorHandler>emptyList());
      try (AggregateMetricSender.Session i = mf.createSession()) {
          i.setDatapoint(
             SignalFxProtocolBuffers.DataPoint.newBuilder()
               .setMetric("curtime")
               .setValue(
                 SignalFxProtocolBuffers.Datum.newBuilder()
                 .setIntValue(System.currentTimeMillis()))
               .addDimensions(
                 SignalFxProtocolBuffers.Dimension.newBuilder()
                   .setKey("source")
                   .setValue("java"))
               .build());
      }

```
