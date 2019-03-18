package com.signalfx.metrics.connection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicNameValuePair;

import com.signalfx.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public abstract class AbstractHttpDataPointProtobufReceiverConnection extends AbstractHttpReceiverConnection implements DataPointReceiver {

    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");

    private final boolean compress;

    public AbstractHttpDataPointProtobufReceiverConnection(SignalFxReceiverEndpoint endpoint,
                                                           int timeoutMs,
                                                           HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
        this.compress = !Boolean.getBoolean(DISABLE_COMPRESSION_PROPERTY);
    }

    @Override
    public void addDataPoints(String auth, List<SignalFxProtocolBuffers.DataPoint> dataPoints)
            throws SignalFxMetricsException {
        if (dataPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        getEntityForVersion(dataPoints),
                        getEndpointForAddDatapoints(),
                        compress);

                int code = resp.getStatusLine().getStatusCode();
                if (code != HttpStatus.SC_OK) {
                    throw new SignalFxMetricsException("Invalid status code " + code);
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to addDataPoints", e);
        }
    }

    protected abstract String getEndpointForAddDatapoints();

    protected abstract HttpEntity getEntityForVersion(
            List<SignalFxProtocolBuffers.DataPoint> dataPoints);

    @Override
    public void backfillDataPoints(String auth, String metric, String metricType, String orgId, Map<String,String> dimensions,
                                   List<SignalFxProtocolBuffers.PointValue> datumPoints)
            throws SignalFxMetricsException {
        if (datumPoints.isEmpty()) {
            return;
        }

        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("orgid", orgId));
        params.add(new BasicNameValuePair("metric_type", metricType));
        params.add(new BasicNameValuePair("metric", metric));

        // Each dimension is added as a param in the form of "sfxdim_DIMNAME"
        for (Map.Entry<String,String> entry : dimensions.entrySet()) {
            params.add(new BasicNameValuePair("sfxdim_" + entry.getKey(), entry.getValue()));
        }

        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth,
                        new InputStreamEntity(
                                new ProtocolBufferStreamingInputStream<SignalFxProtocolBuffers.PointValue>(
                                        datumPoints.iterator()), PROTO_TYPE),
                        "/v1/backfill?" + URLEncodedUtils.format(params, StandardCharsets.UTF_8),
                        false);

                int code = resp.getStatusLine().getStatusCode();
                if (code != HttpStatus.SC_OK) {
                    throw new SignalFxMetricsException("Invalid status code " + code);
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to backfillDataPoints", e);
        }
    }
}
