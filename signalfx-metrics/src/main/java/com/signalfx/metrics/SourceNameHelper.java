package com.signalfx.metrics;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import java.nio.charset.StandardCharsets;

import com.signalfx.metrics.aws.AWSInstanceInfo;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

public final class SourceNameHelper {
    private SourceNameHelper() {
    }

    public static String getDefaultSourceName() {
        String sourceName = System.getProperty("com.signalfx.sourceName");
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName;
        }
        sourceName = System.getenv("SIGNALFX_SOURCE_NAME");
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }
        sourceName = System.getenv("COMPUTERNAME");
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName;
        }
        sourceName = System.getenv("HOSTNAME");
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName;
        }
        throw new RuntimeException(
                "Unable to find a default source name.  Please set one with usingDefaultSource()");
    }

    /**
     * A helper to get the AWS instance ID.  Useful as an alternative default source name.
     * @return AWS instance ID
     * @throws IOException If there are any issues reading the local AWS endpoint.  For example,
     *                     if the host is not on AWS.
     * @deprecated Use {@link AWSInstanceInfo#get()} and set as a dimension on a metric or as an
     *             unique dimension on a SignalFxReporter
     */

    @Deprecated
    public static String getAwsInstanceId() throws IOException {
        URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
        try (InputStream inputStream = url.openStream()) {
            return EntityUtils.toString(new InputStreamEntity(inputStream), StandardCharsets.UTF_8);
        }
    }
}
