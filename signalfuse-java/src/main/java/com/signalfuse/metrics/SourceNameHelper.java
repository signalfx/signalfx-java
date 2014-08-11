package com.signalfuse.metrics;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;

public final class SourceNameHelper {
    private SourceNameHelper() {
    }

    public static String getDefaultSourceName() {
        String sourceName = System.getProperty("com.signalfuse.sourceName");
        if (sourceName != null && !sourceName.isEmpty()) {
            return sourceName;
        }
        sourceName = System.getenv("SIGNALFUSE_SOURCE_NAME");
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
     */
    public static String getAwsInstanceId() throws IOException {
        return IOUtils.toString(new URL("http://169.254.169.254/latest/meta-data/instance-id"));
    }
}
