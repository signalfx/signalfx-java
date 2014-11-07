package com.signalfuse.metrics.connection;

import com.signalfuse.common.proto.ProtocolBufferStreamingInputStream;
import com.signalfuse.metrics.SignalfuseMetricsException;
import com.signalfuse.metrics.endpoint.DataPointReceiverEndpoint;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
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

import java.io.IOException;
import java.util.List;

public abstract class AbstractHttpDataPointProtobufReceiverConnection implements DataPointReceiver {
    // Do not modify this line.  It is auto replaced to a version number.
    public static final String VERSION_NUMBER = "0.0.11-SNAPSHOT";
    static final String USER_AGENT = "SignalFx-java-client/" + VERSION_NUMBER;
    protected static final ContentType PROTO_TYPE = ContentType.create("application/x-protobuf");
    protected static final ContentType JSON_TYPE = ContentType.APPLICATION_JSON;
    private static final Logger log = LoggerFactory
            .getLogger(AbstractHttpDataPointProtobufReceiverConnection.class);
    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final HttpHost host;
    private final RequestConfig requestConfig;

    public AbstractHttpDataPointProtobufReceiverConnection(
            DataPointReceiverEndpoint dataPointEndpoint,
            int timeoutMs) {
        this.host = new HttpHost(dataPointEndpoint.getHostname(), dataPointEndpoint.getPort(),
                dataPointEndpoint.getScheme());
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

    @Override
    public void addDataPoints(String auth, List<SignalFuseProtocolBuffers.DataPoint> dataPoints)
            throws SignalfuseMetricsException {
        if (dataPoints.isEmpty()) {
            return;
        }
        try {
            CloseableHttpResponse resp = null;
            try {
                resp = postToEndpoint(auth, getEntityForVersion(dataPoints),
                        getEndpointForAddDatapoints());
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

    protected abstract String getEndpointForAddDatapoints();

    protected abstract HttpEntity getEntityForVersion(
            List<SignalFuseProtocolBuffers.DataPoint> dataPoints);

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
                        new InputStreamEntity(
                                new ProtocolBufferStreamingInputStream<SignalFuseProtocolBuffers.Datum>(
                                        datumPoints.iterator()), PROTO_TYPE)
                        , "/v1/backfill");
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
}
