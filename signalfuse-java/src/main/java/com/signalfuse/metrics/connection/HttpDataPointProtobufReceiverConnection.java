package com.signalfuse.metrics.connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.signalfuse.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public class HttpDataPointProtobufReceiverConnection implements DataPointReceiver {
    private static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");
    private static final ContentType JSON_TYPE = ContentType.APPLICATION_JSON;
    // Do not modify this line.  It is auto replaced to a version number.
    public static final String VERSION_NUMBER = "0.0.9";
    static final String USER_AGENT = "SignalFx-java-client/" + VERSION_NUMBER;
    private static final Logger log = LoggerFactory
            .getLogger(HttpDataPointProtobufReceiverConnection.class);
    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final HttpHost host;
    private final RequestConfig requestConfig;

    public HttpDataPointProtobufReceiverConnection(DataPointReceiverEndpoint dataPointEndpoint,
                                            int timeoutMs) {
        this.host = new HttpHost(dataPointEndpoint.getHostname(), dataPointEndpoint.getPort(),
                dataPointEndpoint.getScheme());
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
        if (dataPoints.isEmpty()) {
            return;
        }
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
        if (datumPoints.isEmpty()) {
            return;
        }
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
    public Map<String, Boolean> registerMetrics(String auth,
                                Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException {
        Map<String, Boolean> res = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> i: metricTypes.entrySet()) {
            res.put(i.getKey(), false);
        }
        if (metricTypes.isEmpty()) {
            return res;
        }
        List<Map<String, String>> postBodyList = new ArrayList<Map<String, String>>(metricTypes.size());
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> entity : metricTypes
                .entrySet()) {
            postBodyList.add(ImmutableMap.of("sf_metric", entity.getKey(), "sf_metricType", entity.getValue().toString()));
        }

        final byte[] map_as_json;
        try {
            map_as_json = new ObjectMapper().writeValueAsBytes(postBodyList);
        } catch (JsonProcessingException e) {
            throw new SignalfuseMetricsException("Unable to write protocol buffer", e);
        }
        String body = "";
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new ByteArrayInputStream(map_as_json), "/metric?bulkupdate=true",
                        JSON_TYPE);
                try {
                    body = IOUtils.toString(resp.getEntity().getContent());
                } catch (IOException e) {
                    throw new SignalfuseMetricsException("Unable to get reponse content",
                            e);
                }
                if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new SignalfuseMetricsException("Invalid status code "
                            + resp.getStatusLine().getStatusCode() + ": " + body);
                }
                List<Map<String, String>> respObject =
                        new ObjectMapper().readValue(body.getBytes(),
                                new TypeReference<List<Map<String, String>>>(){});
                if (respObject.size() != metricTypes.size()) {
                    throw new SignalfuseMetricsException(
                            String.format("json map mismatch: post_body=%s, resp=%s",
                                    new String(map_as_json), body));
                }
                for (int i=0;i<respObject.size();i++) {
                    Map<String, String> m = respObject.get(i);
                    if (!m.containsKey("code") || "409".equals(m.get("code").toString())) {
                        res.put(postBodyList.get(i).get("sf_metric"), true);
                    }
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalfuseMetricsException(String.format("post_body=%s, resp=%s", new String(map_as_json), body), e);
        }
        return res;
    }
}
