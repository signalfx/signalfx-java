/**
 * Copyright (C) 2016-2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.metrics.connection;

import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;

public class HttpDataPointProtobufReceiverConnectionTest {

  public static final String AUTH_TOKEN = "AUTH_TOKEN";

  @Test
  public void testHttpConnection() throws Exception {
    Server server = new Server(0);
    server.setHandler(new AddPointsHandler());
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
          new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
          .createDataPointReceiver();

      dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
          SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
    }
  }

  @Test
  public void testOptionalAuthToken() throws Exception {
    Server server = new Server(0);
    server.setHandler(new NoAuthTokenExpectedHandler());
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
          new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
          .createDataPointReceiver();

      dpr.addDataPoints(null, Collections.singletonList(
          SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
    }
  }

  @Test
  public void shouldSendDataEvenIfServerClosedConnection() throws Exception {
    int serverIdleTimeout = 5000;

    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setIdleTimeout(serverIdleTimeout);
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    server.setHandler(new AddPointsHandler());
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
          new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
          .createDataPointReceiver();

      dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
          SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));

      Thread.sleep(serverIdleTimeout + 1000);

      dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
          SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
    }
  }

  @Test
  public void shouldRetryOnSocketTimeout() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);
    final int clientTimeoutMs = 100;
    final LatchedTimeoutHandler handler = new LatchedTimeoutHandler(latch, clientTimeoutMs);

    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setIdleTimeout(1000);
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    server.setHandler(handler);
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
              new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
              .setMaxRetries(1)
              .setTimeoutMs(clientTimeoutMs)
              .setNonRetryableExceptions(Collections.emptyList())
              .createDataPointReceiver();
      try {
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
      } catch (Exception ignored2) {
      }
    }

    assertTrue(latch.await(1000, MILLISECONDS));
  }

  @Test
  public void shouldNotRetryOnDefaultNonRetryableExceptions() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final int timeoutMs = 100;
    final LatchedTimeoutHandler handler = new LatchedTimeoutHandler(latch, timeoutMs);

    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setIdleTimeout(1000);
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    server.setHandler(handler);
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
              new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
              .setTimeoutMs(timeoutMs)
              .setMaxRetries(1)
              .createDataPointReceiver();
      try {
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
      } catch (Exception ignored2) {
      }
    }

    assertTrue(latch.await(1000, MILLISECONDS));
  }

  @Test
  public void testBackfill() throws Exception {
    Server server = new Server(0);
    server.setHandler(new BackfillHandler());
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
          new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
          .createDataPointReceiver();

      ArrayList<SignalFxProtocolBuffers.PointValue> values = new ArrayList<SignalFxProtocolBuffers.PointValue>(Arrays.asList(
          SignalFxProtocolBuffers.PointValue.newBuilder().setTimestamp(System.currentTimeMillis())
              .setValue(SignalFxProtocolBuffers.Datum.newBuilder().setDoubleValue(123.0)).build()
      ));
      HashMap<String, String> dims = new HashMap<String, String>();
      dims.put("baz", "gorch");
      dims.put("moo", "cow");

      dpr.backfillDataPoints(AUTH_TOKEN, "foo.bar.baz", "counter", "ABC123", dims, values);
    }
  }

  private static class AddPointsHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
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

      ok(response, baseRequest);
    }
  }

  private static class BackfillHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
      if (!request.getMethod().equals("POST")) {
        error("Incorrect HTTP method for backfill", response, baseRequest);
        return;
      }
      if (!request.getRequestURL().toString().endsWith("/v1/backfill")) {
        error("Incorrect URL for backfill", response, baseRequest);
      }

      List<NameValuePair> params = URLEncodedUtils.parse(baseRequest.getQueryString(), StandardCharsets.UTF_8);
      if (!params.contains(new BasicNameValuePair("orgid", "ABC123"))) {
        error("orgid param is missing for backfill", response, baseRequest);
      }
      if (!params.contains(new BasicNameValuePair("metric_type", "counter"))) {
        error("metric_type is missing for backfill", response, baseRequest);
      }
      if (!params.contains(new BasicNameValuePair("metric", "foo.bar.baz"))) {
        error("metric is missing for backfill", response, baseRequest);
      }
      if (!params.contains(new BasicNameValuePair("sfdim_baz", "gorch"))) {
        error("metric is missing for backfill", response, baseRequest);
      }
      if (!params.contains(new BasicNameValuePair("sfdim_moo", "cow"))) {
        error("metric is missing for backfill", response, baseRequest);
      }

      ok(response, baseRequest);
    }
  }

  private static class NoAuthTokenExpectedHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException {

      if (request.getHeader("X-SF-TOKEN") != null) {
        error("Invalid auth token", response, baseRequest);
        return;
      }

      ok(response, baseRequest);
    }
  }

  private static class LatchedTimeoutHandler extends AbstractHandler {
    private final CountDownLatch latch;
    private final int timeoutMs;

    LatchedTimeoutHandler(CountDownLatch latch, int timeoutMs) {
      this.latch = latch;
      this.timeoutMs = timeoutMs;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
      latch.countDown();
      try {
        Thread.sleep(timeoutMs);
      } catch (Exception ignored) {
      }
    }
  }

  private static void error(String message, HttpServletResponse response, Request baseRequest)
      throws IOException {
    response.setStatus(HttpStatus.SC_BAD_REQUEST);
    response.getWriter().write(message);
    baseRequest.setHandled(true);
  }

  private static void ok(HttpServletResponse response, Request baseRequest)
      throws IOException {
    response.setStatus(HttpStatus.SC_OK);
    response.getWriter().write("\"OK\"");
    baseRequest.setHandled(true);
  }
}
