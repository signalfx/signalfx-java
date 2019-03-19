package com.signalfx.connection;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;

public abstract class AbstractHttpReceiverConnection {

    protected static final Logger log = LoggerFactory.getLogger(AbstractHttpReceiverConnection.class);

    // Do not modify this line.  It is auto replaced to a version number.
    public static final String VERSION_NUMBER = "0.1.1-SNAPSHOT";
    public static final String USER_AGENT = "SignalFx-java-client/" + VERSION_NUMBER;
    public static final String DISABLE_COMPRESSION_PROPERTY = "com.signalfx.public.java.disableHttpCompression";

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final ContentType JSON_TYPE = ContentType.APPLICATION_JSON;

    protected final CloseableHttpClient client;
    protected final HttpHost host;
    protected final RequestConfig requestConfig;

    protected AbstractHttpReceiverConnection(SignalFxReceiverEndpoint endpoint, int timeoutMs,
                                             HttpClientConnectionManager httpClientConnectionManager) {
        this.client = HttpClientBuilder.create()
                .setConnectionManager(httpClientConnectionManager)
                .build();
        this.host = new HttpHost(endpoint.getHostname(), endpoint.getPort(), endpoint.getScheme());

        HttpHost proxy = createHttpProxyFromSystemProperties(endpoint.getHostname());
        this.requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setConnectTimeout(timeoutMs)
                .setProxy(proxy)
                .build();
    }

    protected CloseableHttpResponse postToEndpoint(String auth, HttpEntity entity, String endpoint,
                                                   boolean compress)
            throws IOException {
        if (compress) {
            entity = new GzipCompressingEntity(entity);
        }

        HttpPost post = new HttpPost(String.format("%s%s", host.toURI(), endpoint));
        post.setConfig(requestConfig);
        post.setHeader("X-SF-TOKEN", auth);
        post.setHeader("User-Agent", USER_AGENT);
        post.setEntity(entity);

        try {
            log.trace("Talking to endpoint {}", post);
            return client.execute(post);
        } catch (IOException e) {
            log.trace("Exception trying to execute {}", post, e);
            throw e;
        }
    }

    protected void checkHttpResponse(CloseableHttpResponse resp) throws
            SignalFxMetricsException {
        final String body;
        try {
            body = IOUtils.toString(resp.getEntity().getContent());
        } catch (IOException e) {
            throw new SignalFxMetricsException("Unable to get response content", e);
        }
        if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new SignalFxMetricsException("Invalid status code "
                    + resp.getStatusLine().getStatusCode() + ": " + body);
        }
        if (!"\"OK\"".equals(body)) {
            throw new SignalFxMetricsException("Invalid response body: " + body);
        }
    }

    /**
     * method to create a httphost object based on java network proxy system properties
     *
     * http.proxyHost: the host name of the proxy server
     * http.proxyPort: the port number, the default value being 80
     * http.nonProxyHosts: a list of hosts that should be reached directly, bypassing the proxy.
     *                     This is a list of patterns separated by '|'.
     *                     The patterns may start or end with a '*' for wildcards.
     *                     Any host matching one of these patterns will be reached through a
     *                     direct connection instead of through a proxy.
     *
     * @param endpointHostname  the signalfx endpoint hostname
     *
     * @return an instance of HttpHost based on the java system properties
     *         unless the http proxy host is not configured
     *         OR if the nonProxyHosts rules include this endpoint
     *         then null will be returned instead
     **/
    protected HttpHost createHttpProxyFromSystemProperties(String endpointHostname) {

        String proxyHost = System.getProperty("http.proxyHost");
        if ((proxyHost != null) && (proxyHost.trim().length() > 0)) {

            String nonProxyHosts = System.getProperty("http.nonProxyHosts");
            if (nonProxyHosts != null) {

                // set host strings as regular expressions based on
                // nonProxyHosts rules
                nonProxyHosts = nonProxyHosts.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?");

                // set groups and alternations
                nonProxyHosts = "(" + nonProxyHosts.replaceAll("\\|", ")|(") + ")";

                final Pattern pattern = Pattern.compile(nonProxyHosts);
                if (pattern.matcher(endpointHostname).find()) {
                    // http proxy is not configured for this endpoint
                    return null;
                }
            }

            String proxyPort = System.getProperty("http.proxyPort");
            if ((proxyPort == null) || (proxyPort.trim().length() == 0)) {
                // port 80 is the default in java networking/proxy documentation
                proxyPort = "80";
            }

            // return http proxy host
            return new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort.trim()), "http");
        }

        // http proxy is not configured
        return null;
    }
}
