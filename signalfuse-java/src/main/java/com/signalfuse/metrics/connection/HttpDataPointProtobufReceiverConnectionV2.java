package com.signalfuse.metrics.connection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;

import com.signalfuse.endpoint.SignalFxReceiverEndpoint;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.protobuf.SignalFxProtocolBuffers;

public class HttpDataPointProtobufReceiverConnectionV2
        extends AbstractHttpDataPointProtobufReceiverConnection {
    public HttpDataPointProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
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
            throws SignalfuseMetricsException {
        Map<String, Boolean> res = new HashMap<String, Boolean>();
        for (Map.Entry<String, SignalFxProtocolBuffers.MetricType> i : metricTypes.entrySet()) {
            res.put(i.getKey(), true);
        }
        return res;
    }
}
