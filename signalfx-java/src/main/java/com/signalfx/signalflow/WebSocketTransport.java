/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    protected Integer timeout = DEFAULT_TIMEOUT;
    protected WebSocketClient webSocketClient;
    protected TransportConnection transportConnection;

    protected WebSocketTransport(String token, SignalFxEndpoint endpoint, int apiVersion,
                                 int timeout) {
        this.token = token;
        this.endpoint = endpoint;
        this.path = "/v" + apiVersion + "/signalflow/connect";
        this.timeout = timeout;

        try {
            WebSocketClientFactory factory = new WebSocketClientFactory();
            factory.start();

            this.webSocketClient = factory.newWebSocketClient();

            URIBuilder uriBuilder = new URIBuilder(String.format("%s://%s:%s%s",
                    endpoint.getScheme(), endpoint.getHostname(), endpoint.getPort(), path));

            this.transportConnection = new TransportConnection(token);
            this.webSocketClient.open(uriBuilder.build(), this.transportConnection, timeout,
                    TimeUnit.SECONDS);
        } catch (Exception ex) {
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

        transportConnection.sendMessage(channel, request);

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
        if ((transportConnection.getConnection() != null)
                && (transportConnection.getConnection().isOpen())) {
            transportConnection.close(code, reason);
            try {
                this.webSocketClient.getFactory().stop();
            } catch (Exception ex) {
                log.error("error while stopping websocketfactory", ex);
            }
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

        public WebSocketTransport build() {
            SignalFxEndpoint endpoint = new SignalFxEndpoint(this.protocol, this.host, this.port);
            WebSocketTransport transport = new WebSocketTransport(this.token, endpoint,
                    this.version, this.timeout);
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
     * Value Object that handles binary data message conversion in construction
     */
    protected static class TransportDataMessage {

        protected byte version;
        protected Kind kind;
        protected String channelName;
        protected long logicalTimestampMs;
        protected List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        public TransportDataMessage(byte[] data) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(data);

                this.version = buffer.get(0);
                this.kind = Kind.fromBinaryType(buffer.get(1));
                this.channelName = new String(data, 4, 16, "UTF-8");

                if (this.kind == Kind.DATA) {
                    this.logicalTimestampMs = buffer.getLong(20);

                    byte[] payload = Arrays.copyOfRange(data, 32, data.length);
                    for (int element = 0; element < (payload.length / 17); element++) {

                        int index = element * 17;

                        byte[] tsIdBytes = Arrays.copyOfRange(payload, index + 1, index + 9);
                        String encodedTsId = StringUtils
                                .remove(DatatypeConverter.printBase64Binary(tsIdBytes), "=");

                        Map<String, Object> elementMap = new HashMap<String, Object>(4);
                        elementMap.put("tsId", encodedTsId);

                        switch (payload[index]) {
                        case 1: // long value
                            elementMap.put("value", buffer.getLong(index + 41));
                            break;

                        case 2: // double value
                            elementMap.put("value", buffer.getDouble(index + 41));
                            break;

                        default: // not suppose to happen
                            log.warn("ignoring data message with unknown value type {}",
                                    payload[index]);
                            continue;
                        }

                        this.data.add(elementMap);
                    }
                } else {
                    log.warn("Unsupported binary message type {}", this.kind);
                }
            } catch (Exception ex) {
                log.error("failed to construct transport data message", ex);
            }
        }

        public int getVersion() {
            return version;
        }

        public Kind getKind() {
            return this.kind;
        }

        public String getChannelName() {
            return channelName;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public long getLogicalTimestampMs() {
            return logicalTimestampMs;
        }
    }

    /**
     * WebSocket Transport Connection
     */
    protected static class TransportConnection
            implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        protected static final Logger log = LoggerFactory.getLogger(TransportConnection.class);
        protected String token;
        protected SignalFlowException error;
        protected WebSocket.Connection connection;
        protected Map<String, TransportChannel> channels = Collections
                .synchronizedMap(new HashMap<String, TransportChannel>());
        protected static ObjectMapper objectMapper = new ObjectMapper();
        static {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        public TransportConnection(String token) {
            this.token = token;
        }

        @Override
        public void onClose(int code, String reason) {
            log.debug("websocket connection closed ({} {})", code, reason);

            if (code != 1000) {
                this.error = new SignalFlowException(code, reason);
                log.info("Lost WebSocket connection with {} ({}).", connection, code);

                SignalFlowExceptionStreamMessage errorMessage = new SignalFlowExceptionStreamMessage(
                        this.error);
                for (TransportChannel channel : this.channels.values()) {
                    channel.offer(errorMessage);
                }
            }

            this.channels.clear();
            this.connection = null;
        }

        @Override
        public void onOpen(Connection connection) {
            log.debug("open connection: {}", connection);
            this.connection = connection;

            Map<String, String> authRequest = new HashMap<String, String>();
            authRequest.put("type", "authenticate");
            authRequest.put("token", this.token);

            sendMessage(authRequest);
        }

        @Override
        public void onMessage(byte[] data, int offset, int length) {
            try {
                byte[] messageBytes = Arrays.copyOfRange(data, offset, offset + length);
                TransportDataMessage dataMessage = new TransportDataMessage(messageBytes);

                if (dataMessage.getKind() == Kind.DATA) {
                    LinkedHashMap<String, Object> dataMap = new LinkedHashMap<String, Object>();
                    dataMap.put("logicalTimestampMs", dataMessage.getLogicalTimestampMs());
                    dataMap.put("data", dataMessage.getData());

                    TransportChannel channel = channels.get(dataMessage.getChannelName());
                    if ((channel != null) && (!channel.isClosed())) {
                        StreamMessage streamMessage = new StreamMessage("data", null,
                                objectMapper.writeValueAsString(dataMap));
                        channel.offer(streamMessage);
                    } else {
                        log.debug("ignoring message. channel not found {}",
                                dataMessage.getChannelName());
                    }
                }
            } catch (JsonProcessingException ex) {
                log.error("failed to process messages", ex);
            }
        }

        @Override
        public void onMessage(String data) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = objectMapper.readValue(data, Map.class);

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
                this.connection.sendMessage(message);
            } catch (IOException ex) {
                throw new SignalFlowException("failed to send message", ex);
            }
        }

        public void sendMessage(final Channel channel, final Map<String, String> request) {
            Map<String, String> channelRequest = new HashMap<String, String>(request);
            channelRequest.put("channel", channel.getName());
            try {
                String message = objectMapper.writeValueAsString(channelRequest);
                this.connection.sendMessage(message);
            } catch (IOException ex) {
                throw new SignalFlowException(
                        "failed to send message for channel " + channel.getName(), ex);
            }
        }

        public void add(TransportChannel channel) {
            channels.put(channel.getName(), channel);
        }

        public void remove(TransportChannel channel) {
            channels.remove(channel);
        }

        public void close(int code, String reason) {
            for (Channel channel : this.channels.values()) {
                channel.close();
            }
            this.channels.clear();
            this.connection.close(code, reason);
        }

        public WebSocket.Connection getConnection() {
            return this.connection;
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
