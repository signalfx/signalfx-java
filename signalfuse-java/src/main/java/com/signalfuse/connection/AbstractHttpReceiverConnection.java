package com.signalfuse.connection;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfuse.endpoint.SignalFuseReceiverEndpoint;

public abstract class AbstractHttpReceiverConnection {

    protected static final Logger log = LoggerFactory.getLogger(AbstractHttpReceiverConnection.class);

    // Do not modify this line.  It is auto replaced to a version number.
    public static final String VERSION_NUMBER = "0.0.16-SNAPSHOT";
    public static final String USER_AGENT = "SignalFx-java-client/" + VERSION_NUMBER;
    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final ContentType JSON_TYPE = ContentType.APPLICATION_JSON;

    protected final CloseableHttpClient client;
    protected final HttpHost host;
    protected final RequestConfig requestConfig;

    protected AbstractHttpReceiverConnection(
            SignalFuseReceiverEndpoint endpoint,
            int timeoutMs, HttpClientConnectionManager httpClientConnectionManager) {
        this.client = HttpClientBuilder.create()
                .setConnectionManager(httpClientConnectionManager)
                .build();
        this.host = new HttpHost(endpoint.getHostname(), endpoint.getPort(),
                endpoint.getScheme());
        this.requestConfig = RequestConfig.custom().setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs).setConnectTimeout(timeoutMs).build();
    }

    protected CloseableHttpResponse postToEndpoint(String auth, HttpEntity httpEntity,
                                                   String endpoint)
            throws IOException {
        HttpPost http_post = new HttpPost(String.format("%s%s", host.toURI(), endpoint));
        http_post.setConfig(requestConfig);
        http_post.setHeader("X-SF-TOKEN", auth);
        http_post.setHeader("User-Agent", USER_AGENT);
        http_post.setEntity(httpEntity);

        try {
            log.trace("Talking to endpoint {}", http_post);
            return client.execute(http_post);
        } catch (IOException e) {
            log.trace("Exception trying to execute {}, Exception: {} ", http_post, e);
            throw e;
        }
    }

}
