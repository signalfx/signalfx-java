package com.signalfx.metrics.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;

public class HttpDataPointProtobufReceiverConnectionV2
        extends AbstractHttpDataPointProtobufReceiverConnection {
    public HttpDataPointProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
    }

    public HttpDataPointProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs, int maxRetries,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, maxRetries, httpClientConnectionManager);
    }

    public HttpDataPointProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs, int maxRetries,
            HttpClientConnectionManager httpClientConnectionManager, List<Class<? extends IOException>> nonRetryableExceptions) {
        super(endpoint, timeoutMs, maxRetries, httpClientConnectionManager, nonRetryableExceptions);
    }

    @Override
    protected String getEndpointForAddDatapoints() {
        return "/v2/datapoint";
    }

    @Override
    protected HttpEntity getEntityForVersion(List<SignalFxProtocolBuffers.DataPoint> dataPoints) {
        byte[] bodyBytes = SignalFxProtocolBuffers.DataPointUploadMessage.newBuilder()
                .addAllDatapoints(dataPoints).build().toByteArray();
        return new ByteArrayEntity(bodyBytes, PROTO_TYPE);
    }

    @Override
    public Map<String, Boolean> registerMetrics(String auth,
                                                Map<String, SignalFxProtocolBuffers.MetricType> metricTypes)
            throws SignalFxMetricsException {
        Map<String, Boolean> res = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFxProtocolBuffers.MetricType> i : metricTypes.entrySet()) {
            res.put(i.getKey(), true);
        }
        return res;
    }
}
