package com.splunk.signalfx.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.splunk.signalfx.JsonEncoder;
import com.splunk.signalfx.MetricSender;
import com.splunk.signalfx.Realm;
import com.splunk.signalfx.MultiThreadedApacheClient;

public class CustomSender {

    private static final String TOKEN = "s3cr3t";

    public static void main(String[] args) throws InterruptedException, IOException {
        MetricSender s = MetricSender.builder()
                .realm(Realm.US1)
                .token(TOKEN)
                .client(new MultiThreadedApacheClient(4, 10, TimeUnit.SECONDS)) // default is ApacheClient
                .encoder(new JsonEncoder()) // default is ProtobufEncoder
                .maxBatchSize(10) // default is 1000
                .maxBufferSize(100) // default is 10000
                .maxShutdownWaitMillis(1000) // default is 10000
                .build();
        s.start();
        for (int i = 0; i < 120; i++) {
            s.recordGaugeValue("example.math.mod", 10 + (i % 5), ImmutableMap.of("class", "CustomSender"));
            Thread.sleep(4000);
        }
        s.stop();
    }
}
