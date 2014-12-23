package com.signalfuse.metrics.connection;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.signalfuse.connection.AbstractHttpReceiverConnection;
import com.signalfuse.endpoint.SignalFuseEndpoint;
import com.signalfuse.metrics.protobuf.SignalFuseProtocolBuffers;

public class HttpDataPointProtobufReceiverConnectionTest {
    private static final Logger log = LoggerFactory
            .getLogger(HttpDataPointProtobufReceiverConnectionTest.class);
    public static final String AUTH_TOKEN = "AUTH_TOKEN";

    @Test
    public void testHttpConnection() throws Exception {
        Server server = new Server(0);
        server.setHandler(new MyHandler());
        server.start();
        final int port = server.getConnectors()[0].getLocalPort();
        DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
                new SignalFuseEndpoint("http", "localhost", port)).createDataPointReceiver();
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFuseProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
        server.stop();
    }

    private class MyHandler extends AbstractHandler {
        @Override public void handle(String target, Request baseRequest, HttpServletRequest request,
                                     HttpServletResponse response)
                throws IOException, ServletException {
            if (!request.getHeader("X-SF-TOKEN").equals(AUTH_TOKEN)) {
                error("Invalid auth token", response, baseRequest);
                return;
            }
            if (!request.getHeader("User-Agent")
                    .equals(AbstractHttpReceiverConnection.USER_AGENT)) {
                error("Invalid User agent: " + request.getHeader("User-Agent"), response, baseRequest);
                return;
            }
            SignalFuseProtocolBuffers.DataPointUploadMessage all_datapoints =
                    SignalFuseProtocolBuffers.DataPointUploadMessage.parseFrom(
                            baseRequest.getInputStream());
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
    }
}
