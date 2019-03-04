/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfx.signalflow.StreamMessage.Kind;

/**
 * Base class for stream messages received from a SignalFlow computation.
 *
 * @author dgriff
 */
public abstract class ChannelMessage {

    /**
     * Enumeration of types of channel messages
     */
    public static enum Type {

        STREAM_START(Kind.CONTROL),
        JOB_START(Kind.CONTROL),
        JOB_PROGRESS(Kind.CONTROL),
        CHANNEL_ABORT(Kind.CONTROL),
        END_OF_CHANNEL(Kind.CONTROL),
        INFO_MESSAGE(Kind.INFORMATION),
        METADATA_MESSAGE(Kind.METADATA),
        EXPIRED_TSID_MESSAGE(Kind.EXPIRED_TSID),
        DATA_MESSAGE(Kind.DATA),
        EVENT_MESSAGE(Kind.EVENT),
        ERROR_MESSAGE(Kind.ERROR);

        private final Kind kind;

        Type(Kind kind) {
            this.kind = kind;
        }

        Kind kind() {
            return kind;
        }
    };

    protected static final Logger log = LoggerFactory.getLogger(ChannelMessage.class);
    protected static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected String rawdata;
    protected ChannelMessage.Type channelMessageType;

    public Type getType() {
        return this.channelMessageType;
    }

    public String toString() {
        return this.rawdata;
    }

    /**
     * Converts the raw stream message into the proper type of channel message
     *
     * @param streamMessage
     *            raw stream message
     * @return a channel message instance
     * @throws SignalFlowException
     *             if decode fails
     */
    public static ChannelMessage decodeStreamMessage(StreamMessage streamMessage)
            throws SignalFlowException {
        try {
            ChannelMessage message = null;

            switch (streamMessage.getKind()) {

            case CONTROL:
                message = mapper.readValue(streamMessage.getData(), ControlMessage.class);
                break;

            case INFORMATION:
                message = mapper.readValue(streamMessage.getData(), InfoMessage.class);
                break;

            case METADATA:
                message = mapper.readValue(streamMessage.getData(), MetadataMessage.class);
                break;

            case EXPIRED_TSID:
                message = mapper.readValue(streamMessage.getData(), ExpiredTsIdMessage.class);
                break;

            case DATA:
                message = mapper.readValue(streamMessage.getData(), DataMessage.class);
                break;

            case EVENT:
                message = mapper.readValue(streamMessage.getData(), EventMessage.class);
                break;

            case ERROR:
                message = mapper.readValue(streamMessage.getData(), ErrorMessage.class);
                break;
            }

            if (log.isDebugEnabled()) {
                message.rawdata = streamMessage.getData();
            }

            return message;

        } catch (IOException ex) {
            log.error(streamMessage.toString(), ex);
            throw new SignalFlowException("failed to decode stream message: " + streamMessage, ex);
        }
    }

    /**
     * Base class for control messages.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "event", visible = true)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChannelMessage.StreamStartMessage.class, name = "STREAM_START"),
            @JsonSubTypes.Type(value = ChannelMessage.JobStartMessage.class, name = "JOB_START"),
            @JsonSubTypes.Type(value = ChannelMessage.JobProgressMessage.class, name = "JOB_PROGRESS"),
            @JsonSubTypes.Type(value = ChannelMessage.ChannelAbortMessage.class, name = "CHANNEL_ABORT"),
            @JsonSubTypes.Type(value = ChannelMessage.EndOfChannelMessage.class, name = "END_OF_CHANNEL") })
    public static abstract class ControlMessage extends ChannelMessage {

        protected long timestampMs;

        /**
         * @return The wall clock timestamp (millisecond precision) of the message.
         */
        public long getTimestampMs() {
            return this.timestampMs;
        }
    }

    /**
     * Message received when the stream begins.
     */
    @JsonTypeName("STREAM_START")
    public static class StreamStartMessage extends ControlMessage {

        public StreamStartMessage() {
            this.channelMessageType = Type.STREAM_START;
        }
    }

    /**
     * Message received when the computation completes normally. No further messages will be
     * received from a computation after this one.
     */
    @JsonTypeName("END_OF_CHANNEL")
    public static class EndOfChannelMessage extends ControlMessage {

        public EndOfChannelMessage() {
            this.channelMessageType = Type.END_OF_CHANNEL;
        }
    }

    /**
     * Message received when the SignalFlow computation has started.
     */
    @JsonTypeName("JOB_START")
    public static class JobStartMessage extends ControlMessage {

        protected String handle;

        public JobStartMessage() {
            this.channelMessageType = Type.JOB_START;
        }

        /**
         * @return The computation's handle ID
         */
        public String getHandle() {
            return this.handle;
        }
    }

    /**
     * Message received while computation windows are primed, if they are present. The message will
     * be received multiple times with increasing progress values from 0 to 100, indicating the
     * progress percentage.
     */
    @JsonTypeName("JOB_PROGRESS")
    public static class JobProgressMessage extends ControlMessage {

        protected int progress;

        public JobProgressMessage() {
            this.channelMessageType = Type.JOB_PROGRESS;
        }

        /**
         * @return Computation priming progress, as a percentage between 0 and 100.
         */
        public int getProgress() {
            return this.progress;
        }
    }

    /**
     * Message received when the computation aborted before its defined stop time, either because of
     * an error or from a manual stop. No further messages will be received from a computation after
     * this one.
     */
    @JsonTypeName("CHANNEL_ABORT")
    public static class ChannelAbortMessage extends ControlMessage {

        protected LinkedHashMap<String, String> abortInfo;

        public ChannelAbortMessage() {
            this.channelMessageType = Type.CHANNEL_ABORT;
        }

        /**
         * @return Information about the computation's termination.
         */
        public Map<String, String> getAbortInfo() {
            return this.abortInfo;
        }
    }

    /**
     * Message containing information about the SignalFlow computation's behavior or decisions
     */
    public static class InfoMessage extends ChannelMessage {

        protected LinkedHashMap<String, Object> message;
        protected long logicalTimestampMs;

        public InfoMessage() {
            this.channelMessageType = Type.INFO_MESSAGE;
        }

        /**
         * @return The logical timestamp (millisecond precision) for which the message has been
         *         emitted.
         */
        public long getLogicalTimestampMs() {
            return this.logicalTimestampMs;
        }

        /**
         * @return The information message. Refer to the Developer's documentation for a reference
         *         of the possible messages and their structure.
         */
        public Map<String, Object> getMessage() {
            return this.message;
        }
    }

    /**
     * Message containing metadata information about an output metric or event timeseries. Metadata
     * messages are always emitted by the computation prior to any data or events for the
     * corresponding timeseries.
     */
    public static class MetadataMessage extends ChannelMessage {

        protected LinkedHashMap<String, Object> properties;
        protected String tsId;

        public MetadataMessage() {
            this.channelMessageType = Type.METADATA_MESSAGE;
        }

        /**
         * @return A unique timeseries identifier.
         */
        public String getTsId() {
            return this.tsId;
        }

        /**
         * @return The metadata properties of the timeseries.
         */
        public Map<String, Object> getProperties() {
            return this.properties;
        }
    }

    /**
     * Message informing us that an output timeseries is no longer
     * part of the computation and that we may do some cleanup of
     * whatever internal state we have tied to that output timeseries.
    */
    public static class ExpiredTsIdMessage extends ChannelMessage {

        protected String tsId;

        public ExpiredTsIdMessage() {
            this.channelMessageType = Type.EXPIRED_TSID_MESSAGE;
        }

        /**
         * @return The identifier of the timeseries that's no longer interesting
         *         to the computation.
         */
        public String getTsId() {
            return this.tsId;
        }
    }

    /**
     * Message containing a batch of datapoints generated for a particular iteration.
     */
    public static class DataMessage extends ChannelMessage {

        protected Map<String, Number> data;
        protected long logicalTimestampMs;

        @JsonCreator
        public DataMessage(@JsonProperty("logicalTimestampMs") long logicalTimestampMs,
                           @JsonProperty("data") List<Map<String, Object>> data) {
            this.channelMessageType = Type.DATA_MESSAGE;
            this.logicalTimestampMs = logicalTimestampMs;
            this.data = new HashMap<String, Number>(data.size());
            for (Map<String, Object> datum : data) {
                this.data.put((String) datum.get("tsId"), (Number) datum.get("value"));
            }
        }

        /**
         * @return The logical timestamp of the data (millisecond precision).
         */
        public long getLogicalTimestampMs() {
            return this.logicalTimestampMs;
        }

        /**
         * @return The data, as a map of timeseries ID to datapoint value.
         */
        public Map<String, Number> getData() {
            return this.data;
        }

        public void addData(Map<String, Number> data) {
            this.data.putAll(data);
        }
    }

    /**
     * Message received when the computation has generated an event or alert from a detect block.
     */
    public static class EventMessage extends ChannelMessage {

        protected LinkedHashMap<String, Object> metadata;
        protected LinkedHashMap<String, Object> properties;
        protected long timestampMs;
        protected String tsId;

        public EventMessage() {
            this.channelMessageType = Type.EVENT_MESSAGE;
        }

        /**
         * @return A unique timeseries identifier.
         */
        public String getTsId() {
            return this.tsId;
        }

        /**
         * @return The timestamp of the event (millisecond precision).
         */
        public long getTimestampMs() {
            return this.timestampMs;
        }

        /**
         * @return The metadata of the EventTimeSeries this event belongs to. May be empty if the
         * event was created by the SignalFlow computation itself.
         */
        public Map<String, Object> getMetadata() {
            return this.metadata;
        }

        /**
         * @return The properties of the event. For alerts, you can expect 'was' and 'is' properties
         *         that communicate the evolution of the state of the incident.
         */
        public Map<String, Object> getProperties() {
            return this.properties;
        }
    }

    /**
     * Message received when the computation encounters errors during its initialization.
     * Because the error that is returned might have one of two sets of contents, this has both an
     * `errors` and a `message` accessor. We'll be liberal here so the code can decide how to show
     * this error later. Optimally, these should be different message types, but that's a lot of
     * work to handle on both the backend and client side for +1/-1.
     */
    public static class ErrorMessage extends ChannelMessage {

        protected int error;
        protected ArrayList<Object> errors;
        protected String message;

        public ErrorMessage() {
            this.channelMessageType = Type.ERROR_MESSAGE;
        }

        /**
         * @return The error number, akin to an HTTP error code.
         */
        public int getError() {
            return this.error;
        }

        /**
         * @return The list of errors. Each error has a 'code' defining what the error is, and a
         *         'context' dictionary providing details.
         */
        public List<Object> getErrors() {
            return this.errors;
        }

        /**
         * @return The error message for the failure
         */
        public String getMessage() {
            return this.message;
        }
    }
}
