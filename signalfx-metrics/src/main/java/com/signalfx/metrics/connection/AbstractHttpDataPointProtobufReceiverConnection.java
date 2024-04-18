package com.signalfx.metrics.connection;

import com.signalfx.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.SignalFxMetricsException;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.util.ArrayList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.signalfx.connection.RetryDefaults.DEFAULT_MAX_RETRIES;
import static com.signalfx.connection.RetryDefaults.DEFAULT_NON_RETRYABLE_EXCEPTIONS;

public abstract class AbstractHttpDataPointProtobufReceiverConnection extends AbstractHttpReceiverConnection implements DataPointReceiver {

    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");

    private final boolean compress;

    public AbstractHttpDataPointProtobufReceiverConnection(SignalFxReceiverEndpoint endpoint,
                                                           int timeoutMs,
                                                           HttpClientConnectionManager httpClientConnectionManager) {
        this(endpoint, timeoutMs, DEFAULT_MAX_RETRIES, httpClientConnectionManager, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
    }

    public AbstractHttpDataPointProtobufReceiverConnection(SignalFxReceiverEndpoint endpoint,
                                                           int timeoutMs,
                                                           int maxRetries,
                                                           HttpClientConnectionManager httpClientConnectionManager) {
        this(endpoint, timeoutMs, maxRetries, httpClientConnectionManager, DEFAULT_NON_RETRYABLE_EXCEPTIONS);
    }

    public AbstractHttpDataPointProtobufReceiverConnection(SignalFxReceiverEndpoint endpoint,
                                                           int timeoutMs,
                                                           int maxRetries,
                                                           HttpClientConnectionManager httpClientConnectionManager,
                                                           List<Class<? extends IOException>> nonRetryableExceptions) {
        super(endpoint, timeoutMs, maxRetries, httpClientConnectionManager, nonRetryableExceptions);
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

                int code = resp.getCode();
                // SignalFx may respond with various 2xx return codes for success.
                if (code < 200 || code > 299) {
                    throw new SignalFxMetricsException("Invalid status code " + code);
                }
            } finally {
                if (resp != null) {
                    try {
                        HttpEntity entity = resp.getEntity();
                        EntityUtils.consume(entity);
                    } finally {
                        resp.close();
                    }
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
    public void backfillDataPoints(String auth, String metric, String metricType, String orgId, Map<String, String> dimensions,
                                   List<SignalFxProtocolBuffers.PointValue> datumPoints)
            throws SignalFxMetricsException {
        if (datumPoints.isEmpty()) {
            return;
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("orgid", orgId));
        params.add(new BasicNameValuePair("metric_type", metricType));
        params.add(new BasicNameValuePair("metric", metric));

        // Each dimension is added as a param in the form of "sfxdim_DIMNAME"
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
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

                int code = resp.getCode();
                // SignalFx may respond with various 2xx return codes for success.
                if (code < 200 || code > 299) {
                    throw new SignalFxMetricsException("Invalid status code " + code);
                }
            } finally {
                if (resp != null) {
                    HttpEntity entity = resp.getEntity();
                    if (entity != null) {
                        entity.getContent().close();
                    }
                    resp.close();
                }
            }
        } catch (IOException e) {
            throw new SignalFxMetricsException("Exception posting to backfillDataPoints", e);
        }
    }
}
