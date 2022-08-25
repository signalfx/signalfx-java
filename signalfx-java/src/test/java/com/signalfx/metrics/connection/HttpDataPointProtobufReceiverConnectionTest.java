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
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Assert;
import org.junit.Test;

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
    final CountingTimeoutHandler handler = new CountingTimeoutHandler();
    final int timeout = 1000;
    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setIdleTimeout(timeout);
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    server.setHandler(handler);
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
              new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
              .setMaxRetries(1)
              .setNonRetryableExceptions(Collections.emptyList())
              .createDataPointReceiver();
      try {
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
      } catch (Exception ignored2) {
      }
    }

    Assert.assertEquals(2, handler.requests);
  }

  @Test
  public void shouldNotRetryOnDefaultNonRetryableExceptions() throws Exception{
    final CountingTimeoutHandler handler = new CountingTimeoutHandler();
    final int timeout = 1000;
    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setIdleTimeout(timeout);
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    server.setHandler(handler);
    server.start();

    try (AutoCloseable ignored = server::stop) {
      URI uri = server.getURI();
      DataPointReceiver dpr = new HttpDataPointProtobufReceiverFactory(
              new SignalFxEndpoint(uri.getScheme(), uri.getHost(), uri.getPort()))
              .setMaxRetries(1)
              .createDataPointReceiver();
      try {
        dpr.addDataPoints(AUTH_TOKEN, Collections.singletonList(
                SignalFxProtocolBuffers.DataPoint.newBuilder().setSource("source").build()));
      } catch (Exception ignored2) {
      }
    }

    Assert.assertEquals(1, handler.requests);
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

  private static class CountingTimeoutHandler extends AbstractHandler {
    private int requests = 0;

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException{
      requests++;

      try {
        Thread.sleep(2000);
      } catch (InterruptedException ignored) {
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
