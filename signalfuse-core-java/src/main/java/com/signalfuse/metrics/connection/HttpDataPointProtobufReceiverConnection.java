package com.signalfuse.metrics.connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.signalfuse.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpDataPointProtobufReceiverConnection implements DataPointReceiver {
    private static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");
    private static final ContentType JSON_TYPE = ContentType.APPLICATION_JSON;
    private static final String USER_AGENT = "SignalFx-java-client/0.0.2";
    private static final Logger log = LoggerFactory
            .getLogger(HttpDataPointProtobufReceiverConnection.class);
    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final HttpHost host;
    private final RequestConfig requestConfig;

    HttpDataPointProtobufReceiverConnection(DataPointReceiverEndpoint dataPointEndpoint,
                                            int timeoutMs) {
        this.host = new HttpHost(dataPointEndpoint.getHostname(), dataPointEndpoint.getPort());
        this.requestConfig = RequestConfig.custom().setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs).setConnectTimeout(timeoutMs).build();
    }

    private CloseableHttpResponse postToEndpoint(String auth, InputStream postBodyInputStream,
                                                 String endpoint, ContentType contentType)
            throws IOException {
        HttpPost http_post = new HttpPost(String.format("%s%s", host.toURI(), endpoint));
        http_post.setConfig(requestConfig);
        http_post.setHeader("X-SF-TOKEN", auth);
        http_post.setHeader("User-Agent", USER_AGENT);
        http_post.setEntity(new InputStreamEntity(postBodyInputStream, contentType));

        try {
            log.trace("Talking to endpoint {}", http_post);
            return client.execute(http_post);
        } catch (IOException e) {
            log.trace("Exception trying to execute {}, Exception: {} ", http_post, e);
            throw e;
        }
    }

    @Override
    public void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException {
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new ProtocolBufferStreamingInputStream<SignalFuseProtocolBuffers.DataPoint>(
                                dataPoints.iterator()), "/datapoint",
                        PROTO_TYPE);
                final String body;
                try {
                    body = IOUtils.toString(resp.getEntity().getContent());
                } catch (IOException e) {
                    throw new SignalfuseMetricsException("Unable to get reponse content", e);
                }
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalfuseMetricsException("Invalid status code "
                            + resp.getStatusLine().getStatusCode() + ": " + body);
                }
                if (!"\"OK\"".equals(body)) {
                    throw new SignalfuseMetricsException("Invalid response body: " + body);
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalfuseMetricsException("Exception posting to addDataPoints", e);
        }
    }

    @Override
    public void backfillDataPoints(String auth, String source, String metric,
                                   List<SignalFuseProtocolBuffers.Datum> datumPoints)
            throws SignalfuseMetricsException {
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new ProtocolBufferStreamingInputStream<SignalFuseProtocolBuffers.Datum>(
                                datumPoints.iterator()),
                        "/backfill", PROTO_TYPE);
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalfuseMetricsException(
                            "Invalid status coded " + resp.getStatusLine().getStatusCode());
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalfuseMetricsException("Exception posting to addDataPoints", e);
        }
    }

    @Override
    public void registerMetrics(String auth,
                                Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException {
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> entity : metricTypes
                .entrySet()) {
            Map<String, String> post_body = new HashMap<String, String>(2);
            post_body.put("sf_metric", entity.getKey());
            post_body.put("sf_metricType", entity.getValue().toString());
            final byte[] map_as_json;
            try {
                map_as_json = new ObjectMapper().writeValueAsBytes(post_body);
            } catch (JsonProcessingException e) {
                throw new SignalfuseMetricsException("Unable to write protocol buffer", e);
            }
            try {
                CloseableHttpResponse resp = null;
                try {
                    resp = postToEndpoint(auth,
                            new ByteArrayInputStream(map_as_json), "/metric",
                            JSON_TYPE);
                    if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_CONFLICT
                            && resp.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                        final String body;
                        try {
                            body = IOUtils.toString(resp.getEntity().getContent());
                        } catch (IOException e) {
                            throw new SignalfuseMetricsException("Unable to get reponse content",
                                    e);
                        }
                        throw new SignalfuseMetricsException("Invalid status code "
                                + resp.getStatusLine().getStatusCode() + ": " + body);
                    }
                } finally {
                    if (resp != null) {
                        resp.close();
                    }
                }
            } catch (IOException e) {
                throw new SignalfuseMetricsException(String.format("series=%s, auth=%s, post=%s",
                        auth, post_body, e));
            }
        }
    }
}
