# SignalFuse client libraries [![Build Status](https://travis-ci.org/signalfx/signalfuse-java.svg?branch=master)](https://travis-ci.org/signalfx/signalfuse-java)

This repository contains libraries for instrumenting applications in a
variety of languages and reporting these metrics to SignalFuse. You will
need a SignalFuse account and organization API token to use those. For
more information on SignalFuse and to create an account, go to
http://www.signalfuse.com.

The recommended way to send metrics with java is to use codahale metrics.
If you want to send metrics without integrating with codahale, the module
signalfuse-java will support this.

## Supported languages

* Java 6+ with `signalfuse-metrics`.

## Installation

### With Maven

```xml
<dependency>
  <groupId>com.signalfuse.public</groupId>
  <artifactId>signalfuse-codahale</artifactId>
  <version>0.0.16</version>
</dependency>
```

### From source (Not recommended)

```
$ git clone https://github.com/signalfx/signalfuse-java.git
Cloning into 'signalfuse-java'...
remote: Counting objects: 930, done.
remote: Compressing objects: 100% (67/67), done.
remote: Total 930 (delta 20), reused 0 (delta 0)
Receiving objects: 100% (930/930), 146.79 KiB | 0 bytes/s, done.
Resolving deltas: 100% (289/289), done.
Checking connectivity... done.
$ cd signalfuse-java
$ mvn install
[INFO] Scanning for projects...
...
...
...
[INFO] SignalFuse parent .................................. SUCCESS [  2.483 s]
[INFO] SignalFuse Protocol Buffer definitions ............. SUCCESS [  5.503 s]
[INFO] SignalFuse Protobuf Utilities ...................... SUCCESS [  2.269 s]
[INFO] SignalFuse java libraries .......................... SUCCESS [  3.728 s]
[INFO] Codahale to SignalFuse ............................. SUCCESS [  2.910 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 17.120 s
[INFO] ------------------------------------------------------------------------
```

## Sending metrics

### Codahale

Codahale metrics are the recommended way of integrating your java code with SignalFuse.
More information on the Codahale Metrics library can be found on the
[Codahale Metrics website](https://dropwizard.github.io/metrics/).

#### Setting up Codahale

```java
final MetricRegistry metricRegistery = new MetricRegistry();
final SignalFuseReporter signalfuseReporter = new SignalFuseReporter.Builder(
    metricRegistery,
    "SIGNALFUSE_AUTH_TOKEN"
).build();
signalfuseReporter.start(1, TimeUnit.SECONDS);
final MetricMetadata metricMetadata = signalfuseReporter.getMetricMetadata();
```

#### Sending a metric with Codahale

```java
// This will send the current time in ms to signalfuse as a gauge
        metricRegistery.register("gauge", new Gauge<Long>() {
            public Long getValue() {
                return System.currentTimeMillis;
            }
        });
```

#### Adding Dimensions and SignalFuse metadata to Codahale metrics

You can add SignalFuse specific metadata to codahale metrics by using
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

#### Adding Dimensions without knowing if they already exist

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

#### After setting up Codahale

After setting up a SignalFuseReporter, you can use codahale metrics as
you normally would, reported at the frequency configured to the
`SignalFuseReporter`.

## Sending metrics without using codahale (not recommended)

You can also interact with our java library directly if you do not want to use
 Codahale.  To do this, you will need to build the metric to send to
 signalfuse manually using protocol buffers.
```java
        DataPointReceiverEndpoint dataPointEndpoint = new DataPointEndpoint();
        AggregateMetricSender mf =
                new AggregateMetricSender("test.SendMetrics",
                                          new HttpDataPointProtobufReceiverFactory(
                                                  dataPointEndpoint)
                                                  .setVersion(2),
                                          new StaticAuthToken(auth_token),
                                          Collections.<OnSendErrorHandler>emptyList());
      try (AggregateMetricSender.Session i = mf.createSession()) {
          i.setDatapoint(
             SignalFuseProtocolBuffers.DataPoint.newBuilder()
               .setMetric("curtime")
               .setValue(
                 SignalFuseProtocolBuffers.Datum.newBuilder()
                 .setIntValue(System.currentTimeMillis()))
               .addDimensions(
                 SignalFuseProtocolBuffers.Dimension.newBuilder()
                   .setKey("source")
                   .setValue("java"))
               .build());
      }

```
