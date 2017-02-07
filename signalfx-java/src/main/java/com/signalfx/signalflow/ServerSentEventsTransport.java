/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.connection.AbstractHttpReceiverConnection;
import com.signalfx.endpoint.SignalFxEndpoint;

/**
 * Server-Sent Events transport.
 *
 * Implements a transport to the SignalFlow API that uses simple HTTP requests and reads Server-Sent
 * Events streams back from SignalFx. One connection per SignalFlow computation is required when
 * using this transport. This is a good transport for single, ad-hoc computations. For most use
 * cases though, the WebSocket-based transport is more efficient and has lower latency.
 *
 * @author dgriff
 */
public class ServerSentEventsTransport implements SignalFlowTransport {

    protected static final Logger log = LoggerFactory.getLogger(ServerSentEventsTransport.class);
    public static final Integer DEFAULT_TIMEOUT = 1000;

    protected final String token;
    protected final SignalFxEndpoint endpoint;
    protected final String path;
    protected Integer timeout = DEFAULT_TIMEOUT;

    protected ServerSentEventsTransport(String token, final SignalFxEndpoint endpoint,
                                        final int apiVersion, final Integer timeout) {
        this.token = token;
        this.endpoint = endpoint;
        this.path = "/v" + apiVersion + "/signalflow";
        this.timeout = timeout;
    }

    @Override
    public Channel attach(String handle, final Map<String, String> parameters) {
        if (log.isDebugEnabled()) {
            log.debug("attach: [ {} ] with parameters: {}", handle, parameters);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);

            response = connection.post(this.token, this.path + "/" + handle + "/attach", parameters,
                    null);

            return new TransportChannel(connection, response);
        } catch (Exception ex) {
            close(response);
            close(connection);
            throw new SignalFlowException("failed to create transport channel for attach", ex);
        }
    }

    @Override
    public Channel execute(String program, final Map<String, String> parameters)
            throws SignalFlowException {
        if (log.isDebugEnabled()) {
            log.debug("execute: [ {} ] with parameters: {}", program, parameters);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);

            response = connection.post(this.token, this.path + "/execute", parameters, program);

            return new TransportChannel(connection, response);
        } catch (IOException ioex) {
            close(response);
            close(connection);
            throw new SignalFlowException("failed to create transport channel for execute", ioex);
        }
    }

    @Override
    public Channel preflight(String program, final Map<String, String> parameters)
            throws SignalFlowException {
        if (log.isDebugEnabled()) {
            log.debug("preflight: [ {} ] with parameters: {}", program, parameters);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);

            response = connection.post(this.token, this.path + "/preflight", parameters, program);

            return new TransportChannel(connection, response);
        } catch (IOException ioex) {
            close(response);
            close(connection);
            throw new SignalFlowException("failed to create transport channel for execute", ioex);
        }
    }
    @Override
    public void start(String program, final Map<String, String> parameters) {
        if (log.isDebugEnabled()) {
            log.debug("start: [ {} ] with parameters: {}", program, parameters);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);
            response = connection.post(this.token, this.path + "/start", parameters, program);
        } catch (Exception ex) {
            throw new SignalFlowException("failed to start program - " + program, ex);
        } finally {
            close(response);
            close(connection);
        }
    }

    @Override
    public void stop(String handle, final Map<String, String> parameters) {
        if (log.isDebugEnabled()) {
            log.debug("stop: [ {} ] with parameters: {}", handle, parameters);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);
            response = connection.post(this.token, this.path + "/" + handle + "/stop", parameters,
                    null);
        } catch (Exception ex) {
            throw new SignalFlowException("failed to stop program - " + handle, ex);
        } finally {
            close(response);
            close(connection);
        }
    }

    @Override
    public void keepalive(String handle) {
        if (log.isDebugEnabled()) {
            log.debug("keepalive: [ {} ]", handle);
        }

        TransportConnection connection = null;
        CloseableHttpResponse response = null;
        try {
            connection = new TransportConnection(this.endpoint, timeout);
            response = connection.post(this.token, this.path + "/" + handle + "/keepalive", null,
                    null);
        } catch (Exception ex) {
            throw new SignalFlowException("failed to set keepalive for program - " + handle, ex);
        } finally {
            close(response);
            close(connection);
        }
    }

    @Override
    public void close(int code, String reason) {
        // nothing to close (separate connections are used and closed by the channel using it)
    }

    private void close(CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException ioex) {
            log.error("error closing response", ioex);
        }
    }

    private void close(TransportConnection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException ioex) {
            log.error("error closing transport connection", ioex);
        }
    }

    /**
     * Builder of SSE Transport Instance
     */
    public static class TransportBuilder {

        private String token;
        private String protocol = "https";
        private String host = SignalFlowTransport.DEFAULT_HOST;
        private int port = 443;
        private int timeout = 1;
        private int version = 2;

        public TransportBuilder(String token) {
            this.token = token;
        }

        public TransportBuilder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public TransportBuilder setHost(String host) {
            this.host = host;
            return this;
        }

        public TransportBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public TransportBuilder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public TransportBuilder setAPIVersion(int version) {
            this.version = version;
            return this;
        }

        public ServerSentEventsTransport build() {
            SignalFxEndpoint endpoint = new SignalFxEndpoint(this.protocol, this.host, this.port);
            ServerSentEventsTransport transport = new ServerSentEventsTransport(this.token,
                    endpoint, this.version, this.timeout * 1000);
            return transport;
        }
    }

    /**
     * SSE Transport Connection
     */
    public static class TransportConnection extends AbstractHttpReceiverConnection {

        protected static final Logger log = LoggerFactory.getLogger(TransportConnection.class);
        public static final int DEFAULT_TIMEOUT_MS = 1000;
        protected final RequestConfig transportRequestConfig;

        public TransportConnection(SignalFxEndpoint endpoint) {
            this(endpoint, DEFAULT_TIMEOUT_MS);
        }

        public TransportConnection(SignalFxEndpoint endpoint, int timeoutMs) {
            super(endpoint, timeoutMs, new BasicHttpClientConnectionManager());

            this.transportRequestConfig = RequestConfig.custom().setSocketTimeout(0)
                    .setConnectionRequestTimeout(this.requestConfig.getConnectionRequestTimeout())
                    .setConnectTimeout(this.requestConfig.getConnectTimeout())
                    .setProxy(this.requestConfig.getProxy()).build();

            log.debug("constructed request config: {}", this.transportRequestConfig.toString());
        }

        public CloseableHttpResponse post(String token, String path,
                                          final Map<String, String> parameters, String body)
                throws SignalFlowException {
            HttpPost httpPost = null;
            try {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                if (parameters != null) {
                    for (Map.Entry<String, String> entry : parameters.entrySet()) {
                        params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                    }
                }

                URIBuilder uriBuilder = new URIBuilder(String.format("%s%s", host.toURI(), path));
                uriBuilder.addParameters(params);

                httpPost = new HttpPost(uriBuilder.build());
                httpPost.setConfig(transportRequestConfig);
                httpPost.setHeader("X-SF-TOKEN", token);
                httpPost.setHeader("User-Agent", USER_AGENT);
                httpPost.setHeader("Content-Type", "text/plain");
                if (body != null) {
                    HttpEntity httpEntity = new StringEntity(body);
                    httpPost.setEntity(httpEntity);
                }

                if (log.isDebugEnabled()) {
                    log.debug(httpPost.toString());
                }

                CloseableHttpResponse response = client.execute(httpPost);

                StatusLine statusLine = response.getStatusLine();
                int statuscode = statusLine.getStatusCode();
                if ((statuscode < 200) || (statuscode >= 300)) {

                    try {
                        response.close();
                    } catch (IOException ex) {
                        log.error("failed to close response", ex);
                    }

                    String errorMessage = statusLine.getStatusCode() + ": failed post [ " + httpPost
                            + " ] reason: " + statusLine.getReasonPhrase();
                    throw new SignalFlowException(statusLine.getStatusCode(), errorMessage);
                }

                return response;
            } catch (IOException ex) {
                throw new SignalFlowException("failed communication. " + ex.getMessage(), ex);
            } catch (URISyntaxException ex) {
                throw new SignalFlowException("invalid uri. " + ex.getMessage(), ex);
            }
        }

        public void close() throws IOException {
            client.close();
        }
    }

    /**
     * Computation channel fed from a Server-Sent Events stream.
     */
    public static class TransportChannel extends Channel {

        protected static final Logger log = LoggerFactory.getLogger(TransportChannel.class);

        private TransportConnection connection;
        private CloseableHttpResponse response;
        private HttpEntity responseHttpEntity;
        private TransportEventStreamParser streamParser;

        public TransportChannel(final TransportConnection connection,
                                final CloseableHttpResponse response)
                throws IOException {
            super();
            this.connection = connection;
            this.response = response;
            this.responseHttpEntity = response.getEntity();
            this.streamParser = new TransportEventStreamParser(
                    this.responseHttpEntity.getContent());
            this.iterator = this.streamParser;

            log.debug("constructed {} of type {}", this, this.getClass().getName());
        }

        @Override
        public void close() {
            super.close();

            try {
                this.response.close();
            } catch (IOException ex) {
                log.error("failed to close response", ex);
            }

            try {
                this.connection.close();
            } catch (IOException ex) {
                log.error("failed to close connection", ex);
            }

            this.streamParser.close();
        }
    }

    public static class TransportEventStreamParser implements Iterator<StreamMessage>, Closeable {

        protected static final Logger log = LoggerFactory
                .getLogger(TransportEventStreamParser.class);

        private static final String EVENT = "event";
        private static final String ID = "id";
        private static final String DATA = "data";
        private static final String RETRY = "retry";
        private static final String DEFAULT_EVENT = "message";
        private static final String EMPTY_STRING = "";
        private static final Pattern DIGITS_ONLY = Pattern.compile("^[\\d]+$");

        private BufferedReader eventStreamReader;
        private boolean endOfStreamReached = false;

        private int reconnectionTimeoutMs = 1000; // default is 1 second
        private StreamMessage nextMessage;
        private String lastEventId;
        private String eventNameBuffer = DEFAULT_EVENT;
        private StringBuilder dataBuffer = new StringBuilder();

        public TransportEventStreamParser(final InputStream eventStream)
                throws UnsupportedEncodingException {
            this.eventStreamReader = new BufferedReader(
                    new InputStreamReader(eventStream, "UTF-8"));
        }

        public String getLastEventId() {
            return this.lastEventId;
        }

        public int getReconnectionTimeoutMs() {
            return this.reconnectionTimeoutMs;
        }

        @Override
        public boolean hasNext() {
            while ((endOfStreamReached == false) && (eventStreamReader != null)
                    && (nextMessage == null)) {
                parseNext();
            }

            return nextMessage != null;
        }

        @Override
        public StreamMessage next() {
            while ((endOfStreamReached == false) && (eventStreamReader != null)
                    && (nextMessage == null)) {
                parseNext();
            }

            if (nextMessage != null) {
                StreamMessage message = this.nextMessage;

                // important to set next message to null here as that variable stores the next
                // message (if one exists) which is checked by next and hasNext methods. and we just
                // popped the last message off so it should be null now.
                this.nextMessage = null;

                return message;
            } else {
                throw new NoSuchElementException("no more stream messages");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove from stream not supported");
        }

        @Override
        public void close() {
            if (this.eventStreamReader != null) {
                try {
                    this.eventStreamReader.close();
                    this.eventStreamReader = null;
                } catch (IOException ex) {
                    log.error("failed to close event stream", ex);
                }
            }
        }

        private void parseNext() {
            if (eventStreamReader != null) {
                try {
                    long startTime = System.currentTimeMillis();
                    dataBuffer.setLength(0);

                    String line;
                    while ((line = eventStreamReader.readLine()) != null) {
                        int colonIndex;
                        if (line.trim().isEmpty()) {
                            // message ready for dispatch
                            break;
                        } else if (line.startsWith(":")) {
                            // ignore the line
                        } else if ((colonIndex = line.indexOf(":")) != -1) {
                            String field = line.substring(0, colonIndex);
                            String value = line.substring(colonIndex + 1).replaceFirst(" ",
                                    EMPTY_STRING);
                            processField(field, value);
                        } else {
                            processField(line.trim(), EMPTY_STRING);
                        }
                    }

                    if (line == null) {
                        // end of stream reached
                        endOfStreamReached = true;
                        close();
                    }

                    if (dataBuffer.length() > 0) {
                        String data = dataBuffer.toString();
                        if (data.endsWith("\n")) {
                            data = data.substring(0, data.length() - 1);
                        }

                        nextMessage = new StreamMessage(eventNameBuffer, lastEventId, data);

                    } else {
                        log.debug(eventNameBuffer.toString());
                        eventNameBuffer = EMPTY_STRING;
                        nextMessage = null;
                    }

                    log.debug("total stream message read/parse time (ms): {}",
                            (System.currentTimeMillis() - startTime));

                } catch (IOException ex) {
                    log.error("failed to parse next stream event", ex);
                    throw new SignalFlowException("failed to parse next stream event", ex);
                }
            } else {
                nextMessage = null;
            }
        }

        private void processField(String field, String value) {
            if (DATA.equals(field)) {
                dataBuffer.append(value).append("\n");
            } else if (ID.equals(field)) {
                lastEventId = value;
            } else if (EVENT.equals(field)) {
                eventNameBuffer = value;
            } else if (RETRY.equals(field)) {
                if (DIGITS_ONLY.matcher(value).matches()) {
                    // set event stream's reconnection time to integer value
                    reconnectionTimeoutMs = Integer.parseInt(value);
                }
            }
        }
    }
}
