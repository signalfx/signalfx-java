package com.signalfx.metrics.connection;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;

import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class HttpEventProtobufReceiverConnectionV2
        extends AbstractHttpEventProtobufReceiverConnection {
    public HttpEventProtobufReceiverConnectionV2(
            SignalFxReceiverEndpoint endpoint, int timeoutMs,
            HttpClientConnectionManager httpClientConnectionManager) {
        super(endpoint, timeoutMs, httpClientConnectionManager);
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
