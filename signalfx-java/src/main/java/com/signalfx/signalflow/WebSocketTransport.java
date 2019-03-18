/*
 * Copyright (C) 2016-2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.Uninterruptibles;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.signalflow.ChannelMessage.Type;
import com.signalfx.signalflow.StreamMessage.Kind;

/**
 * WebSocket based transport.
 *
 * Uses the SignalFlow WebSocket connection endpoint to interact with SignalFx's SignalFlow API.
 * Multiple computation streams can be multiplexed through a single, pre-opened WebSocket
 * connection. It also utilizes a more efficient binary encoding for data so it requires less
 * bandwidth and has overall less latency.
 *
 * @author dgriff
 */
public class WebSocketTransport implements SignalFlowTransport {

    protected static final Logger log = LoggerFactory.getLogger(WebSocketTransport.class);
    public static final int DEFAULT_TIMEOUT = 1; // 1 second

    protected final String token;
    protected final SignalFxEndpoint endpoint;
    protected final String path;
    protected final int timeout;
    protected final boolean compress;
    protected WebSocketClient webSocketClient;
    protected TransportConnection transportConnection;

    protected WebSocketTransport(String token, SignalFxEndpoint endpoint, int apiVersion,
                                 int timeout, boolean compress, int maxBinaryMessageSize) {
        this.token = token;
        this.endpoint = endpoint;
        this.path = "/v" + apiVersion + "/signalflow/connect";
        this.timeout = timeout;
        this.compress = compress;

        try {
            this.transportConnection = new TransportConnection(token);
            URI uri = new URIBuilder(String.format("%s://%s:%s%s", endpoint.getScheme(),
                    endpoint.getHostname(), endpoint.getPort(), path)).build();

            this.webSocketClient = new WebSocketClient(new SslContextFactory());
            if (maxBinaryMessageSize > 0) {
                this.webSocketClient.getPolicy().setMaxBinaryMessageSize(maxBinaryMessageSize);
            }
            if (timeout > 0) {
                this.webSocketClient.setConnectTimeout(TimeUnit.SECONDS.toMillis(timeout));
            }
            this.webSocketClient.start();
            this.webSocketClient.connect(this.transportConnection, uri);
            this.transportConnection.awaitConnected(timeout, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (this.webSocketClient != null) {
                try {
                    this.webSocketClient.stop();
                } catch (Exception e) {
                    log.warn("error closing websocket client", e);
                }
            }
            throw new SignalFlowException("failed to construct websocket transport", ex);
        }
    }

    @Override
    public Channel attach(String handle, Map<String, String> parameters) {
        log.debug("attach: [ {} ] with parameters: {}", handle, parameters);

        Channel channel = new TransportChannel(transportConnection);

        Map<String, String> request = new HashMap<String, String>(parameters);
        request.put("type", "attach");
        request.put("handle", handle);
        request.put("compress", Boolean.toString(compress));

        transportConnection.sendMessage(channel, request);

        return channel;
    }

    @Override
    public Channel execute(String program, Map<String, String> parameters) {
        log.debug("execute: [ {} ] with parameters: {}", program, parameters);

        Channel channel = new TransportChannel(transportConnection);
        HashMap<String, String> request = new HashMap<String, String>(parameters);
        request.put("type", "execute");
        request.put("program", program);
        request.put("compress", Boolean.toString(compress));

        transportConnection.sendMessage(channel, request);

        return channel;
    }

    @Override
    public Channel preflight(String program, Map<String, String> parameters) {
        log.debug("preflight: [ {} ] with parameters: {}", program, parameters);

        Channel channel = new TransportChannel(transportConnection);
        HashMap<String, String> request = new HashMap<String, String>(parameters);
        request.put("type", "preflight");
        request.put("program", program);

        transportConnection.sendMessage(channel, parameters);

        return channel;
    }

    @Override
    public void start(String program, Map<String, String> parameters) {
        log.debug("start: [ {} ] with parameters: {}", program, parameters);

        HashMap<String, String> request = new HashMap<String, String>(parameters);
        request.put("type", "start");
        request.put("program", program);

        transportConnection.sendMessage(request);
    }

    @Override
    public void stop(String handle, Map<String, String> parameters) {
        log.debug("stop: [ {} ] with parameters: {}", handle, parameters);

        HashMap<String, String> request = new HashMap<String, String>(parameters);
        request.put("type", "stop");
        request.put("handle", handle);

        transportConnection.sendMessage(request);
    }

    @Override
    public void close(int code, String reason) {
        if (transportConnection.getSession() != null && transportConnection.getSession().isOpen()) {
            transportConnection.close(code, reason);
            log.debug("transport closed");
        }
    }

    @Override
    public void keepalive(String handle) {
        log.debug("keepalive: [ {} ]", handle);

        HashMap<String, String> request = new HashMap<String, String>();
        request.put("type", "keepalive");
        request.put("handle", handle);

        transportConnection.sendMessage(request);
    }

    /**
     * Builder of WebSocket Transport Instance
     */
    public static class TransportBuilder {

        private String token;
        private String protocol = "wss";
        private String host = SignalFlowTransport.DEFAULT_HOST;
        private int port = 443;
        private int timeout = DEFAULT_TIMEOUT;
        private int version = 2;
        private boolean compress = true;
        private int maxBinaryMessageSize = -1;

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

        public TransportBuilder useCompression(boolean compress) {
            this.compress = compress;
            return this;
        }

        public TransportBuilder setMaxBinaryMessageSize(int size) {
            this.maxBinaryMessageSize = size;
            return this;
        }

        public WebSocketTransport build() {
            SignalFxEndpoint endpoint = new SignalFxEndpoint(this.protocol, this.host, this.port);
            WebSocketTransport transport = new WebSocketTransport(this.token, endpoint,
                    this.version, this.timeout, this.compress, this.maxBinaryMessageSize);
            return transport;
        }
    }

    /**
     * Special type of StreamMessage for conveying websocket/connection errors to channels
     */
    protected static class SignalFlowExceptionStreamMessage extends StreamMessage {

        protected SignalFlowException exception;

        public SignalFlowExceptionStreamMessage(final SignalFlowException exception) {
            super("error", null, exception.getMessage());
            this.exception = exception;
        }

        public SignalFlowException getException() {
            return this.exception;
        }
    }

    /**
     * WebSocket Transport Connection
     */
    protected static class TransportConnection extends WebSocketAdapter {

        private static final Logger log = LoggerFactory.getLogger(TransportConnection.class);

        private static final Charset ASCII = Charset.forName("US-ASCII");
        private static final Charset UTF_8 = Charset.forName("UTF-8");
        private static final BaseEncoding base64Encoder = BaseEncoding.base64Url().omitPadding();
        private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};

        private static final int MAX_CHANNEL_NAME_LENGTH = 16;
        private static final int BINARY_PREAMBLE_LENGTH = 4;
        private static final int BINARY_HEADER_LENGTH = 20;

        private static final int LONG_TYPE = 0x01;
        private static final int DOUBLE_TYPE = 0x02;
        private static final int INT_TYPE = 0x03;

        private static final ObjectMapper objectMapper = new ObjectMapper();
        static {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        private final CountDownLatch latch = new CountDownLatch(1);
        private final String token;
        private final Map<String, TransportChannel> channels = Collections
                .synchronizedMap(new HashMap<String, TransportChannel>());
        private SignalFlowException error;

        protected TransportConnection(String token) {
            this.token = token;
        }

        @Override
        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
            log.debug("websocket connected to {}", session.getRemoteAddress());

            Map<String, String> authRequest = new HashMap<String, String>();
            authRequest.put("type", "authenticate");
            authRequest.put("token", this.token);

            sendMessage(authRequest);
        }

        @Override
        public void onWebSocketClose(int code, String reason) {
            log.debug("websocket connection closed ({} {})", code, reason);

            if (code != 1000) {
                this.error = new SignalFlowException(code, reason);
                log.info("Lost WebSocket connection with {} ({}).", getSession().getRemoteAddress(),
                        code);

                SignalFlowExceptionStreamMessage errorMessage = new SignalFlowExceptionStreamMessage(
                        this.error);
                for (TransportChannel channel : this.channels.values()) {
                    channel.offer(errorMessage);
                }
            }

            this.channels.clear();
            super.onWebSocketClose(code, reason);
        }

        @Override
        public void onWebSocketBinary(byte[] data, int offset, int length) {
            byte version = data[offset];
            byte type;
            byte flags;

            // Decode message type and flags from header
            switch (version) {
            case 1:
                // +--------------+--------------+--------------+--------------+
                // | Version      | Message type | Flags        | Reserved     |
                type = data[offset + 1];
                flags = data[offset + 2];
                break;
            case 2:
                // +--------------+--------------+--------------+--------------+
                // |           Version           | Message type | Flags        |
                type = data[offset + 2];
                flags = data[offset + 3];
                break;
            default:
                log.error("ignoring message with unsupported encoding version {}", version);
                return;
            }

            Kind kind;
            try {
                kind = Kind.fromBinaryType(type);
            } catch (IllegalArgumentException iae) {
                log.error("ignoring message with unsupported type {}", type);
                return;
            }

            // Channel name is the 16 bytes following the binary preamble in the header.
            String channelName = new String(data, offset + BINARY_PREAMBLE_LENGTH,
                    MAX_CHANNEL_NAME_LENGTH, ASCII);
            // Everything after that is the body of the message.
            byte[] body = Arrays.copyOfRange(data, offset + BINARY_HEADER_LENGTH, offset + length);

            boolean compressed = (flags & (1 << 0)) != 0;
            if (compressed) {
                ByteArrayInputStream bais = new ByteArrayInputStream(body);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    GZIPInputStream gzip = new GZIPInputStream(bais);
                    try {
                        IOUtils.copy(gzip, baos);
                    } finally {
                        IOUtils.closeQuietly(gzip);
                    }
                    body = baos.toByteArray();
                } catch (IOException ioe) {
                    log.error("failed to process message", ioe);
                    return;
                } finally {
                    IOUtils.closeQuietly(baos);
                    IOUtils.closeQuietly(bais);
                }
            }

            boolean json = (flags & (1 << 1)) != 0;
            if (json) {
                onWebSocketText(new String(body, UTF_8));
                return;
            }

            Map<String, Object> message = null;
            switch (kind) {
            case DATA:
                message = decodeBinaryDataMessage(version, body);
                break;
            default:
                log.error("ignoring message with unsupported binary encoding of kind {}", kind);
                return;
            }

            if (message != null) {
                TransportChannel channel = channels.get(channelName);
                if (channel != null && !channel.isClosed()) {
                    try {
                        StreamMessage streamMessage = new StreamMessage("data", null,
                                objectMapper.writeValueAsString(message));
                        channel.offer(streamMessage);
                    } catch (JsonProcessingException ex) {
                        log.error("failed to process message", ex);
                    }
                } else {
                    log.debug("ignoring message. channel not found {}", channelName);
                }
            }
        }

        private static Map<String, Object> decodeBinaryDataMessage(byte version, byte[] data) {
            try {
                Map<String, Object> message = new HashMap<String, Object>();
                ByteBuffer buffer = ByteBuffer.wrap(data);
                switch (version) {
                case 1:
                    message.put("logicalTimestampMs", buffer.getLong());
                    break;
                case 2:
                    message.put("logicalTimestampMs", buffer.getLong());
                    message.put("maxDelayMs", buffer.getLong());
                    break;
                }

                int count = buffer.getInt();
                List<Map<String, Object>> datapoints = new ArrayList<Map<String, Object>>(count);
                for (int element = 0; element < count; element++) {
                    Map<String, Object> elementMap = new HashMap<String, Object>(3);

                    byte type = buffer.get();
                    byte[] tsIdBytes = new byte[8];
                    buffer.get(tsIdBytes);
                    elementMap.put("tsId", base64Encoder.encode(tsIdBytes));

                    switch (type) {
                    case LONG_TYPE:
                    case INT_TYPE: // int or long value
                        elementMap.put("value", buffer.getLong());
                        break;
                    case DOUBLE_TYPE: // double value
                        elementMap.put("value", buffer.getDouble());
                        break;
                    default:
                        log.warn("ignoring data message with unknown value type {}", type);
                        return null;
                    }

                    datapoints.add(elementMap);
                }
                message.put("data", datapoints);
                return message;
            } catch (Exception ex) {
                log.error("failed to construct transport data message", ex);
                return null;
            }
        }

        @Override
        public void onWebSocketText(String data) {
            try {
                // Incoming text message is expected to be JSON.
                Map<String, Object> dataMap = objectMapper.readValue(data, MAP_TYPE_REF);

                // Intercept KEEP_ALIVE messages
                String event = (String) dataMap.get("event");
                if ("KEEP_ALIVE".equals(event)) {
                    return;
                }

                String type = (String) dataMap.get("type");
                if (type == null) {
                    log.debug("type missing so ignoring message. {}", dataMap);
                    return;
                }

                // Authenticated messages inform us that our authentication has been accepted
                // and we can now consider the socket as "connected".
                if (type.equals("authenticated")) {
                    log.info("WebSocket connection authenticated as {} (in {})",
                            dataMap.get("userId"), dataMap.get("orgId"));
                    this.latch.countDown();
                } else {
                    // All other messages should have a channel.
                    String channelName = (String) dataMap.get("channel");
                    if (channelName != null) {
                        TransportChannel channel = channels.get(channelName);
                        if ((channel != null) && (!channel.isClosed())) {
                            StreamMessage message = new StreamMessage(type, null, data);
                            channel.offer(message);
                        } else {
                            log.debug("ignoring message. channel not found {}", channelName);
                        }
                    }
                }
            } catch (IOException ex) {
                log.error("failed to process messages", ex);
            }
        }

        public void sendMessage(final Map<String, String> request) {
            try {
                String message = objectMapper.writeValueAsString(request);
                this.getRemote().sendString(message);
            } catch (Exception ex) {
                throw new SignalFlowException("failed to send message", ex);
            }
        }

        public void sendMessage(final Channel channel, final Map<String, String> request) {
            try {
                Map<String, String> channelRequest = new HashMap<String, String>(request);
                channelRequest.put("channel", channel.getName());
                String message = objectMapper.writeValueAsString(channelRequest);
                this.getRemote().sendString(message);
            } catch (Exception ex) {
                throw new SignalFlowException(
                        "failed to send message for channel " + channel.getName(), ex);
            }
        }

        public void add(TransportChannel channel) {
            this.channels.put(channel.getName(), channel);
        }

        public void remove(TransportChannel channel) {
            this.channels.remove(channel);
        }

        public void close(int code, String reason) {
            for (Channel channel : this.channels.values()) {
                channel.close();
            }
            this.channels.clear();
            this.getSession().close(code, reason);
            this.latch.countDown();
        }

        public void awaitConnected(long timeout, TimeUnit unit) throws TimeoutException {
            if (!Uninterruptibles.awaitUninterruptibly(this.latch, timeout, unit)) {
                throw new TimeoutException("timeout establishing connection");
            }
        }
    }

    /**
     * Computation channel fed from a Server-Sent Events stream.
     */
    protected static class TransportChannel extends Channel {

        protected static final Logger log = LoggerFactory.getLogger(TransportChannel.class);
        protected TransportConnection connection;
        protected Queue<StreamMessage> messageQueue = new ConcurrentLinkedQueue<StreamMessage>();
        protected TransportEventStreamParser parser = new TransportEventStreamParser(messageQueue);

        public TransportChannel(TransportConnection sharedConnection) {
            super();
            this.connection = sharedConnection;
            this.iterator = parser;
            this.connection.add(this); // register channel with transport connection
            log.debug("constructed {} of type {}", this.toString(), this.getClass().getName());
        }

        public boolean offer(final StreamMessage message) {
            return messageQueue.offer(message);
        }

        @Override
        public void close() {
            super.close();
            this.connection.remove(this); // deregister channel with transport connection
        }
    }

    /**
     * Iterator over stream messages from websocket connection for a channel
     */
    protected static class TransportEventStreamParser implements Iterator<StreamMessage> {

        protected Queue<StreamMessage> messageQueue;
        protected boolean isClosed = false;

        public TransportEventStreamParser(Queue<StreamMessage> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public boolean hasNext() {
            return isClosed == false;
        }

        @Override
        public StreamMessage next() {
            StreamMessage streamMessage = null;
            while ((!isClosed) && (streamMessage == null)) {

                streamMessage = messageQueue.poll();
                if (streamMessage != null) {

                    switch (streamMessage.getKind()) {

                    case CONTROL:
                        ChannelMessage channelMessage = ChannelMessage
                                .decodeStreamMessage(streamMessage);
                        if ((channelMessage.getType() == Type.END_OF_CHANNEL)
                                || (channelMessage.getType() == Type.CHANNEL_ABORT)) {
                            close(); // this is the last message for computation
                        }
                        break;

                    case ERROR:
                        if (streamMessage instanceof SignalFlowExceptionStreamMessage) {
                            close(); // no more messages now
                            throw ((SignalFlowExceptionStreamMessage) streamMessage).getException();
                        }
                        break;

                    default:
                    }

                } else {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ex) {
                        close();
                    }
                }
            }

            if (streamMessage != null) {
                return streamMessage;
            } else {
                throw new NoSuchElementException("no more stream messages");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove from stream not supported");
        }

        public void close() {
            this.isClosed = true;
        }
    }
}
