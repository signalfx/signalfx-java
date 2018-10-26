/**
 * Copyright (C) 2016-2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.metrics.connection;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

public class HttpDataPointProtobufReceiverConnectionTest {

    public static final String AUTH_TOKEN = "AUTH_TOKEN";

    @Test
    public void testHttpConnection() throws Exception {
        Server server = new Server(0);
        server.setHandler(new MyHandler());
        server.start();
        URI uri = server.getURI();
        DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
                new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
                .createDataPointReceiver();
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
        server.stop();
    }

    private class MyHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {
            if (!request.getHeader("X-SF-TOKEN").equals(AUTH_TOKEN)) {
                error("Invalid auth token", response, baseRequest);
                return;
            }
            if (!request.getHeader("User-Agent")
                    .equals(AbstractHttpReceiverConnection.USER_AGENT)) {
                error("Invalid User agent: " + request.getHeader("User-Agent") + " vs "
                        + AbstractHttpReceiverConnection.USER_AGENT, response, baseRequest);
                return;
            }
            SignalFxProtocolBuffers.DataPointUploadMessage all_datapoints =
                    SignalFxProtocolBuffers.DataPointUploadMessage.parseFrom(
                            new GZIPInputStream(baseRequest.getInputStream()));
            if (!all_datapoints.getDatapoints(0).getSource().equals("source")) {
                error("Invalid datapoint source", response, baseRequest);
                return;
            }
            response.setStatus(HttpStatus.SC_OK);
            response.getWriter().write("\"OK\"");
            baseRequest.setHandled(true);
        }

        private void error(String message, HttpServletResponse response, Request baseRequest)
                throws IOException {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            response.getWriter().write(message);
            baseRequest.setHandled(true);
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean isStarting() {
            return false;
        }

        @Override
        public boolean isStopping() {
            return false;
        }

        @Override
        public boolean isStopped() {
            return false;
        }

        @Override
        public boolean isFailed() {
            return false;
        }
    }
}
