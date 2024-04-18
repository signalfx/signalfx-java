package com.signalfx.metrics.connection;

import java.util.List;

import org.apache.hc.client5.http.io.HttpClientConnectionManager;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;

public class HttpEventProtobufReceiverConnectionV2
        extends AbstractHttpEventProtobufReceiverConnection {
    public HttpEventProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
    }

    public HttpEventProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs, int maxRetries,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, maxRetries, httpClientConnectionManager);
    }

    @Override
    protected String getEndpointForAddEvents() {
        return "/v2/event";
    }

    @Override
    protected HttpEntity getEntityForVersion(List<SignalFxProtocolBuffers.Event> events) {
        byte[] bodyBytes = SignalFxProtocolBuffers.EventUploadMessage.newBuilder()
                .addAllEvents(events).build().toByteArray();
        return new ByteArrayEntity(bodyBytes, PROTO_TYPE);
    }
}
