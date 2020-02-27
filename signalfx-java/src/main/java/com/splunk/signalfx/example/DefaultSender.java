package com.splunk.signalfx.example;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import com.splunk.signalfx.MetricSender;
import com.splunk.signalfx.Realm;

public class DefaultSender {

    private static final int STEPS = 120;
    private static final String TOKEN = "s3cr3t";

    public static void main(String[] args) throws IOException, InterruptedException {
        MetricSender s = MetricSender.builder()
                .withDefaults()
                .realm(Realm.US1)
                .token(TOKEN)
                .build();
        s.start();
        for (int i = 0; i < STEPS + 1; i++) {
            double value = 1.0 + Math.sin(2 * Math.PI * i / STEPS);
            s.recordGaugeValue("example.math.sine", value, ImmutableMap.of("class", "DefaultSender"));
            Thread.sleep(1000);
        }
        s.stop();
    }
}
