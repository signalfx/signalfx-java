package com.signalfx.endpoint;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters that specify how to connect to SignalFx API endpoint
 * 
 * @author jack
 */
public class SignalFxEndpoint implements SignalFxReceiverEndpoint {
    public static final String DEFAULT_SCHEME = "https";
    public static final String DEFAULT_HOSTNAME = "ingest.signalfx.com";
    public static final int DEFAULT_PORT = 443;
    private static final Logger log = LoggerFactory.getLogger(SignalFxEndpoint.class);
    
    /**
     * API protocol scheme - http or https
     */
    private final String scheme;
    
    /**
     * API hostname
     */
    private final String hostname;
    
    /**
     * TCP port
     */
    private final int port;

    public SignalFxEndpoint(String hostname, int port) {
        this(getDefaultScheme(), hostname, port);
    }

    public SignalFxEndpoint(String scheme, String hostname, int port) {
        this.scheme = scheme;
        this.hostname = hostname;
        this.port = port;
    }
    
    public SignalFxEndpoint() {
        this(getDefaultScheme(), getDefaultHostname(), getDefaultPort());
    }

    private static String getPropertyOrEnv(String propertyName, String envName, String fallback) {
        return StringUtils.defaultIfEmpty(System.getProperty(propertyName, System.getenv(envName)),
                fallback);
    }

    private static String getDefaultScheme() {
        return getPropertyOrEnv("com.signalfx.api.scheme", "SIGNALFX_API_SCHEME", DEFAULT_SCHEME);
    }

    private static String getDefaultHostname() {
        return getPropertyOrEnv("com.signalfx.api.hostname",
                                "SIGNALFX_API_HOSTNAME", DEFAULT_HOSTNAME);
    }

    private static int getDefaultPort() throws NumberFormatException {
        final String foundPort = getPropertyOrEnv("com.signalfx.api.port",
                                    "SIGNALFX_API_PORT", Integer.toString(DEFAULT_PORT));
        try {
            return Integer.parseInt(foundPort);
        } catch (NumberFormatException e) {
            log.error("Invalid found port >>{}<<", foundPort, e);
            throw e;
        }
    }

    @Override public String getScheme() {
        return scheme;
    }
    
    @Override public String getHostname() {
        return hostname;
    }

    @Override public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return getScheme() + "://" + getHostname() + ':' + getPort();
    }
}
