# SignalFx Java client library

This library is intended to eventually replace AggregateMetricSender and friends.

## Goals

* Ease of use
* Pluggable API
* Protocol independence
* Performance

## Default Sender

```java
import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import com.splunk.signalfx.MetricSender;
import com.splunk.signalfx.Realm;

public class DefaultSender {

    private static final int STEPS = 120;
    private static final String TOKEN = "S3CR3T";

    public static void main(String[] args) throws IOException, InterruptedException {
        MetricSender s = MetricSender.builder()
                .withDefaults()
                .realm(Realm.US1)
                .token(TOKEN)
                .build();
        s.start();
        for (int i = 0; i < STEPS + 1; i++) {
            double value = 1.0 + Math.sin(2 * Math.PI * i / STEPS);
            s.recordGaugeValue("example.math.sine", value, ImmutableMap.of("class", "SimpleExample"));
            Thread.sleep(1000);
        }
        s.stop();
    }
}
```

Before you can send metrics to SignalFx, you'll need to know the realm for your account and the ingest token
for your organization. To get the realm, click the avatar at the upper right of the SignalFx web application
and select **My Profile**. Your realm will be indicated towards the bottom of that page. To get the ingest token,
then click **Access Tokens** on the left navbar, open the **Default** panel, and click **Show Token**.

To send metrics to SignalFx, you first have to create a `MetricSender` by using a `MetricSender.Builder`. The
Builder can give you a `MetricSender` with sensible defaults, provided you call `withDefaults()`.

Once constructed, a `MetricSender` object must be `start()`ed to start the thread that reads queued messages
and hands them off to the HTTP client.

Once started, it is thread safe and should be able to handle a typical volume of messages, sending
them in small batches, as soon as they become available. When you call methods on `MetricSender` like
`recordGaugeValue` they enqueue datapoints for immediate - but asynchronous - sending. Typically, these datapoint
methods don't 'block' but there is a limit to how much the queue can store (by default this limit is 10k
messages). When this limit is reached, attempts to enqueue datapoints via methods like `recordGaugeValue` will
block until the queue has enough space.

The default configuration uses a single-threaded HttpClient, which should be acceptable for most use cases. However,
if more bandwidth is needed, there is an available `MultiThreadedApacheClient` which can be passed into the Builder
via `client(HttpClient client)`. It uses an executor service to make requests in parallel but can make no guarantees
that messages are sent in order.

## Custom Sender

```java
package com.splunk.signalfx.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.splunk.signalfx.JsonEncoder;
import com.splunk.signalfx.MetricSender;
import com.splunk.signalfx.Realm;
import com.splunk.signalfx.MultiThreadedApacheClient;

public class CustomSender {

    public static void main(String[] args) throws InterruptedException, IOException {
        MetricSender s = MetricSender.builder()
                .realm(Realm.US1)
                .token("OBbVSVSQQSlWjLU3SeiKGQ")
                .client(new MultiThreadedApacheClient(4, 10, TimeUnit.SECONDS)) // default is ApacheClient
                .encoder(new JsonEncoder()) // default is ProtobufEncoder
                .maxBatchSize(10) // default is 1000
                .maxBufferSize(100) // default is 10000
                .maxShutdownWaitMillis(1000) // default is 10000
                .build();
        s.start();
        for (int i = 0; i < 120; i++) {
            s.recordGaugeValue("example.math.mod", 10 + (i % 5), ImmutableMap.of("class", "CustomSender"));
            Thread.sleep(1000);
        }
        s.stop();
    }
}
```

Example code above can be found in the example package.
