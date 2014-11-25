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
  <version>0.0.12</version>
</dependency>
```

### From source (Not recommended)

```
$ git clone https://github.com/signalfx/signalfuse-java.git
$ cd signalfuse-java
$ mvn install
```

## Sending metrics

### Codahale

Codahale metrics are the recommended way of integrating your java code with SignalFuse.
More information on the Codahale Metrics library can be found on the
[Codahale Metrics website](https://dropwizard.github.io/metrics/).

#### Setting up Codahale

```java
MetricRegistry registry = new MetricRegistry();
new SignalFuseReporter.Builder(
    registry,
    "SIGNALFUSE_AUTH_TOKEN"
).build().start(1, TimeUnit.SECONDS);
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

#### Adding SignalFuse metadata to Codahale metrics

You can add SignalFuse specific metadata to codahale metrics by using
the MetricMetadata of the reporter.

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
