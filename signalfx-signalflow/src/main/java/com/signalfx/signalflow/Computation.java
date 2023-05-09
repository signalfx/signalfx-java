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
import com.signalfx.signalflow.ChannelMessage.ExpiredTsIdMessage;
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
    protected boolean isAttachedChannel;

    private Map<String, Map<String, Object>> metadata = new HashMap<String, Map<String, Object>>();

    private String id;
    private Channel channel;
    private ChannelMessage nextMessage;
    private State state = State.STATE_UNKNOWN;
    private long lastLogicalTimestampMs = -1;
    private long resolution;
    private int expectedBatches;
    private boolean batchCountDetected;
    private int currentBatchCount;
    private DataMessage currentBatchMessage;

    public Computation(SignalFlowTransport transport, String program, Map<String, String> params,
                       boolean attach) {
        this.transport = transport;
        this.program = program;
        this.params = params;
        this.isAttachedChannel = attach;
        this.channel = isAttachedChannel ? attach() : execute();
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
    public long getResolution() {
        return resolution;
    }

    /**
     * @return current computation state
     */
    public State getState() {
        return state;
    }

    /**
     * @return last message time in milliseconds since midnight, January 1, 1970 UTC
     */
    public long getLastLogicalTimestampMs() {
        return lastLogicalTimestampMs;
    }

    /**
     * @return sorted list of known timeseries ids
     */
    public Collection<String> getKnownTSIDs() {
        List<String> list = new ArrayList<String>(metadata.keySet());
        Collections.sort(list);
        return list;
    }

    /**
     * @param tsid
     *            unique identifier of timeseries
     * @return the full metadata object for the given timeseries (by its ID), or null if not
     *         available.
     */
    public Map<String, Object> getMetadata(String tsid) {
        return metadata.get(tsid);
    }

    /**
     * Getter of iterator that iterates over the messages from the computation's output.
     */
    public Iterator<ChannelMessage> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() throws ComputationAbortedException,
           ComputationFailedException, SignalFlowException, StreamRequestException {
        while ((state != State.STATE_COMPLETED) && (!channel.isClosed) && (nextMessage == null)) {
            parseNext();
        }

        return nextMessage != null;
    }

    /**
     * Iterate over the messages from the computation's output.
     *
     * Control and metadata messages are intercepted and interpreted to enhance this Computation's
     * object knowledge of the computation's context. Data and event messages are yielded back to
     * the caller as a generator.
     */
    @Override
    public ChannelMessage next() throws ComputationAbortedException, ComputationFailedException,
            SignalFlowException, NoSuchElementException {
        while ((state != State.STATE_COMPLETED) && (!channel.isClosed) && (nextMessage == null)) {
            parseNext();
        }

        if (nextMessage != null) {
            ChannelMessage message = nextMessage;
            nextMessage = null;
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
        if (lastLogicalTimestampMs >= 0) {
            params.put("start", Long.toString(lastLogicalTimestampMs));
        }

        return transport.execute(program, params);
    }

    /**
     * Attach to existing channel for computation
     *
     * @return Channel for computation
     * @throws SignalFlowException
     *             if transport fails to attach to channel
     */
    private Channel attach() throws SignalFlowException {
        return transport.attach(program, params);
    }

    /**
     * Process the channel messages to manage computation
     *
     * @throws ComputationAbortedException
     *             on receiving channel message aborted
     * @throws ComputationFailedException
     *             on receiving channel message error
     */
    private void parseNext() throws ComputationAbortedException,
            ComputationFailedException, SignalFlowException {
        nextMessage = null;
        while (state != State.STATE_COMPLETED) {
            if (!channel.hasNext()) {
                if (state != State.STATE_COMPLETED) {
                    channel.close();
                    channel = isAttachedChannel ? attach() : execute();
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
                    nextMessage = message;
                    id = ((JobStartMessage) message).getHandle();
                    break;

                case JOB_PROGRESS:
                    nextMessage = message;
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
                    MetadataMessage metadataMessage = (MetadataMessage) message;
                    metadata.put(metadataMessage.getTsId(), metadataMessage.getProperties());
                    nextMessage = message;
                    break;

                case EXPIRED_TSID_MESSAGE:
                    // Intercept expired-tsid messages to clean it up.
                    ExpiredTsIdMessage expiredTsIdMessage = (ExpiredTsIdMessage) message;
                    metadata.remove(expiredTsIdMessage.getTsId());
                    nextMessage = message;
                    break;

                case INFO_MESSAGE:
                    InfoMessage infoMessage = (InfoMessage) message;
                    String messageCode = (String) infoMessage.getMessage().get("messageCode");

                    // Extract the output resolution from the appropriate message, if it's present.
                    if ("JOB_RUNNING_RESOLUTION".equals(messageCode)) {
                        @SuppressWarnings("unchecked")
                        LinkedHashMap<String, Object> contents = (LinkedHashMap<String, Object>) infoMessage
                                .getMessage().get("contents");
                        resolution = ((Number) contents.get("resolutionMs")).longValue();
                    }

                    batchCountDetected = true;
                    if (currentBatchMessage != null) {
                        setNextDataMessageToYield();
                    }
                    break;

                case DATA_MESSAGE:
                    // Accumulate data messages and release them when we have received
                    // all batches for the same logical timestamp.
                    state = State.STATE_DATA_RECEIVED;
                    if (!batchCountDetected) {
                        expectedBatches++;
                    }

                    DataMessage dataMessage = (DataMessage) message;
                    if (currentBatchMessage == null) {
                        currentBatchMessage = dataMessage;
                        currentBatchCount = 1;
                    } else if (dataMessage.getLogicalTimestampMs() == currentBatchMessage
                            .getLogicalTimestampMs()) {
                        currentBatchMessage.addData(dataMessage.getData());
                        currentBatchCount++;
                    } else {
                        batchCountDetected = true;
                    }

                    if (batchCountDetected && currentBatchMessage != null
                            && currentBatchCount == expectedBatches) {
                        setNextDataMessageToYield();
                    }
                    break;

                case EVENT_MESSAGE:
                    nextMessage = message;
                    break;

                case ERROR_MESSAGE:
                    ErrorMessage errorMessage = (ErrorMessage) message;
                    /* This is a hack based on the fact that the API can return type different
                     * error messages with the same type. We have to check attributes to know
                     * which error we're working with.
                     */
                    if (errorMessage.getMessage() != null) {
                        throw new StreamRequestException(errorMessage.getError(), errorMessage.getMessage());
                    } else {
                        throw new ComputationFailedException(errorMessage.getErrors());
                    }
                }
            }

            if (nextMessage != null) {
                break;
            }
        }
    }

    /**
     * Set the next data message that will be returned by the iterator and reset the current batch
     * message in which we accumulate.
     */
    private void setNextDataMessageToYield() {
        DataMessage yieldMessage = currentBatchMessage;
        currentBatchMessage = null;
        currentBatchCount = 0;
        lastLogicalTimestampMs = yieldMessage.getLogicalTimestampMs();
        nextMessage = yieldMessage;
    }
}
