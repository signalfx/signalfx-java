package com.signalfuse.metrics.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.signalfuse.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpDataPointProtobufReceiverConnection
        extends AbstractHttpDataPointProtobufReceiverConnection {
    public HttpDataPointProtobufReceiverConnection(
            DataPointReceiverEndpoint dataPointEndpoint, int timeoutMs) {
        super(dataPointEndpoint, timeoutMs);
    }

    @Override
    protected HttpEntity getEntityForVersion(List<SignalFuseProtocolBuffers.DataPoint> dataPoints) {
        return new InputStreamEntity(
                new ProtocolBufferStreamingInputStream<SignalFuseProtocolBuffers.DataPoint>(
                        dataPoints.iterator()), PROTO_TYPE);
    }

    protected String getEndpointForAddDatapoints() {
        return "/v1/datapoint";
    }

    @Override
    public Map<String, Boolean> registerMetrics(String auth,
                                                Map<String, SignalFuseProtocolBuffers.MetricType> metricTypes)
            throws SignalfuseMetricsException {
        Map<String, Boolean> res = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> i : metricTypes.entrySet()) {
            res.put(i.getKey(), false);
        }
        if (metricTypes.isEmpty()) {
            return res;
        }
        List<Map<String, String>> postBodyList = new ArrayList<Map<String, String>>(
                metricTypes.size());
        for (Map.Entry<String, SignalFuseProtocolBuffers.MetricType> entity : metricTypes
                .entrySet()) {
            postBodyList.add(ImmutableMap
                    .of("sf_metric", entity.getKey(), "sf_metricType",
                            entity.getValue().toString()));
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
                        new ByteArrayEntity(map_as_json, JSON_TYPE), "/v1/metric?bulkupdate=true");
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
                                new TypeReference<List<Map<String, String>>>() {
                                });
                if (respObject.size() != metricTypes.size()) {
                    throw new SignalfuseMetricsException(
                            String.format("json map mismatch: post_body=%s, resp=%s",
                                    new String(map_as_json), body));
                }
                for (int i = 0; i < respObject.size(); i++) {
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
            throw new SignalfuseMetricsException(
                    String.format("post_body=%s, resp=%s", new String(map_as_json), body), e);
        }
        return res;
    }
}
