/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.signalfx.signalflow.ChannelMessage.ChannelAbortMessage;
import com.signalfx.signalflow.ChannelMessage.DataMessage;
import com.signalfx.signalflow.ChannelMessage.ErrorMessage;
import com.signalfx.signalflow.ChannelMessage.InfoMessage;
import com.signalfx.signalflow.ChannelMessage.JobStartMessage;
import com.signalfx.signalflow.ChannelMessage.MetadataMessage;

/**
 * A live handle to a running SignalFlow computation.
 * 
 * @author dgriff
 */
public class Computation implements Iterable<ChannelMessage>, Iterator<ChannelMessage> {

    /**
     * Enumeration of computation states
     */
    public static enum State {
        STATE_UNKNOWN,
        STATE_STREAM_STARTED,
        STATE_COMPUTATION_STARTED,
        STATE_DATA_RECEIVED,
        STATE_COMPLETED,
        STATE_ABORTED;
    }

    protected SignalFlowTransport transport;
    protected String program;
    protected Map<String, String> params;
    protected boolean isAttachedChannel = false;

    private Channel channel;
    private ChannelMessage nextMessage = null;
    private State state = State.STATE_UNKNOWN;
    private Long lastLogicalTimestampMs;
    private String id;
    private Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();
    private Integer resolution;
    private int expectedBatches = 0;
    private boolean batchCountDetected = false;
    private int currentBatchCount = 0;
    private DataMessage currentBatchMessage = null;

    public Computation(SignalFlowTransport transport, String program, Map<String, String> params,
                       boolean attach) {
        this.transport = transport;
        this.program = program;
        this.params = params;
        this.isAttachedChannel = attach;

        if (isAttachedChannel) {
            this.channel = attach();
        } else {
            this.channel = execute();
        }
    }

    /**
     * @return handle to computation
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return data resolution
     */
    public Integer getResolution() {
        return this.resolution;
    }

    /**
     * @return current computation state
     */
    public State getState() {
        return this.state;
    }

    /**
     * @return last message time in milliseconds since midnight, January 1, 1970 UTC
     */
    public Long getLastLogicalTimestampMs() {
        return this.lastLogicalTimestampMs;
    }

    /**
     * @return sorted list of known timeseries ids
     */
    public Collection<String> getKnownTSIDs() {
        if (metadata != null) {
            List<String> list = new ArrayList<String>(metadata.keySet());
            Collections.sort(list);
            return list;
        }
        return null;
    }

    /**
     * @param tsid
     *            unique identifier of timeseries
     * @return the full metadata object for the given timeseries (by its ID), or null if not
     *         available.
     */
    public Map<String, Object> getMetadata(String tsid) {
        if (this.metadata != null) {
            return this.metadata.get(tsid);
        }
        return null;
    }

    /**
     * Getter of iterator that iterates over the messages from the computation's output.
     */
    public Iterator<ChannelMessage> iterator() {
        return this;
    }

    @Override
    public boolean hasNext()
            throws ComputationAbortedException, ComputationFailedException, SignalFlowException {

        while ((state != State.STATE_COMPLETED) && (!channel.isClosed) && (nextMessage == null)) {
            parseNext();
        }

        return nextMessage != null;
    }

    /**
     * Iterate over the messages from the computation's output. Control and metadata messages are
     * intercepted and interpreted to enhance this Computation's object knowledge of the
     * computation's context. Data and event messages are yielded back to the caller as a generator.
     */
    @Override
    public ChannelMessage next() throws ComputationAbortedException, ComputationFailedException,
            SignalFlowException, NoSuchElementException {

        while ((state != State.STATE_COMPLETED) && (!channel.isClosed) && (nextMessage == null)) {
            parseNext();
        }

        if (nextMessage != null) {
            ChannelMessage message = this.nextMessage;

            // important to set next message to null here
            this.nextMessage = null;

            return message;
        } else {
            // no more messages can come from this channel
            throw new NoSuchElementException("no more stream messages");
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }

    /**
     * Manually close this computation and detach from its stream. This computation object cannot be
     * restarted, used or streamed for after this method is called.
     */
    public void close() {
        channel.close();
        nextMessage = null;
    }

    /**
     * Create channel for computation
     * 
     * @return Channel for computation
     * @throws SignalFlowException
     *             if transport fails to create channel
     */
    private Channel execute() throws SignalFlowException {

        HashMap<String, String> params = new HashMap<String, String>(this.params);
        if (lastLogicalTimestampMs != null) {
            params.put("start", Long.toString(lastLogicalTimestampMs));
        }

        return this.transport.execute(program, params);
    }

    /**
     * Attach to existing channel for computation
     * 
     * @return Channel for computation
     * @throws SignalFlowException
     *             if transport fails to attach to channel
     */
    private Channel attach() throws SignalFlowException {

        return this.transport.attach(program, params);
    }

    /**
     * Process the channel messages to manage computation
     * 
     * @throws ComputationAbortedException
     *             on receiving channel message aborted
     * @throws ComputationFailedException
     *             on receiving channel message error
     */
    private void parseNext()
            throws ComputationAbortedException, ComputationFailedException, SignalFlowException {

        this.nextMessage = null;
        while (state != State.STATE_COMPLETED) {

            if (!channel.hasNext()) {

                if (state != State.STATE_COMPLETED) {
                    this.channel.close();
                    if (this.isAttachedChannel) {
                        this.channel = attach();
                    } else {
                        this.channel = execute();
                    }
                    continue;
                }

            } else {

                ChannelMessage message = channel.next();

                switch (message.channelMessageType) {

                case STREAM_START:
                    state = State.STATE_STREAM_STARTED;
                    break;

                case JOB_START:
                    state = State.STATE_COMPUTATION_STARTED;
                    this.nextMessage = message;
                    JobStartMessage jobStartMessage = (JobStartMessage) message;
                    this.id = jobStartMessage.getHandle();
                    break;

                case JOB_PROGRESS:
                    this.nextMessage = message;
                    break;

                case CHANNEL_ABORT:
                    state = State.STATE_ABORTED;
                    ChannelAbortMessage abortMessage = (ChannelAbortMessage) message;
                    throw new ComputationAbortedException(abortMessage.getAbortInfo());

                case END_OF_CHANNEL:
                    state = State.STATE_COMPLETED;
                    break;

                case METADATA_MESSAGE:
                    // Intercept metadata messages to accumulate received metadata.
                    // TODO(dgriff): this can accumulate metadata without bounds if a computation
                    // has a high rate of member churn.
                    MetadataMessage metadataMessage = (MetadataMessage) message;
                    metadata.put(metadataMessage.getTsId(), metadataMessage.getProperties());
                    this.nextMessage = message;
                    break;

                case INFO_MESSAGE:
                    InfoMessage infoMessage = (InfoMessage) message;
                    String messageCode = (String) infoMessage.getMessage().get("messageCode");

                    // Extract the output resolution from the appropriate message, if it's present.
                    if ("JOB_RUNNING_RESOLUTION".equals(messageCode)) {
                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, Object> contents = (LinkedHashMap<String, Object>) infoMessage
                                .getMessage().get("contents");
                        this.resolution = (Integer) contents.get("contents");
                    }
                    this.batchCountDetected = true;
                    if (this.currentBatchMessage != null) {
                        DataMessage yieldMessage = this.currentBatchMessage;
                        this.currentBatchMessage = null;
                        this.currentBatchCount = 0;
                        this.lastLogicalTimestampMs = yieldMessage.getLogicalTimestampMs();
                        this.nextMessage = yieldMessage;
                    }
                    break;

                case DATA_MESSAGE:
                    // Accumulate data messages and release them when we have received
                    // all batches for the same logical timestamp.
                    state = State.STATE_DATA_RECEIVED;
                    if (!this.batchCountDetected) {
                        this.expectedBatches++;
                    }

                    DataMessage dataMessage = (DataMessage) message;
                    if (this.currentBatchMessage == null) {
                        this.currentBatchMessage = dataMessage;
                        this.currentBatchCount = 1;
                    } else {
                        if ((dataMessage.getLogicalTimestampMs()
                                .longValue() == this.currentBatchMessage.getLogicalTimestampMs()
                                        .longValue())
                                && (this.currentBatchCount < this.expectedBatches)) {
                            this.currentBatchMessage.addData(dataMessage.getData());
                            this.currentBatchCount++;
                        } else {
                            this.batchCountDetected = true;
                        }
                    }

                    if (this.currentBatchMessage != null) {
                        DataMessage yieldMessage = this.currentBatchMessage;
                        this.currentBatchMessage = null;
                        this.currentBatchCount = 0;
                        this.lastLogicalTimestampMs = yieldMessage.getLogicalTimestampMs();
                        this.nextMessage = yieldMessage;
                    }
                    break;

                case EVENT_MESSAGE:
                    this.nextMessage = message;
                    break;

                case ERROR_MESSAGE:
                    ErrorMessage errorMessage = (ErrorMessage) message;
                    throw new ComputationFailedException(errorMessage.getErrors());
                }
            }

            if (this.nextMessage != null) {
                break;
            }
        }
    }
}
