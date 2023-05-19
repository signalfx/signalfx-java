package com.signalfx.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
}
