/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.signalfx.signalflow.ChannelMessage.DataMessage;
import com.signalfx.signalflow.ChannelMessage.EventMessage;
import com.signalfx.signalflow.ChannelMessage.JobProgressMessage;
import com.signalfx.signalflow.ChannelMessage.JobStartMessage;
import com.signalfx.signalflow.ChannelMessage.MetadataMessage;
import com.signalfx.signalflow.Computation.State;

/**
 * Class provides basic plumbing used by subclasses to processing computation.
 * 
 * subclass the onMessage methods and invoke the process method to run on current
 * thread or use executor to submit as callable in another thread.
 * 
 * @author dgriff
 */
public abstract class ComputationHandler implements Callable<Computation> {

    protected static final Logger log = LoggerFactory.getLogger(ComputationHandler.class);
    protected Computation computation;
    private Long startTimeMs;
    private Long stopTimeMs;

    /**
     * Constructor that sets the computation
     * 
     * @param computation
     *            instance to process
     */
    public ComputationHandler(Computation computation) {
        this.computation = computation;
    }

    /**
     * Override to process job start messages
     * 
     * @param message
     *            job start
     */
    protected void onMessage(JobStartMessage message) {}

    /**
     * Override to process job progress messages
     * 
     * @param message
     *            job progress
     */
    protected void onMessage(JobProgressMessage message) {}

    /**
     * Override to process data messages
     * 
     * @param message
     *            data
     */
    protected void onMessage(DataMessage message) {}

    /**
     * Override to process event messages
     * 
     * @param message
     *            event
     */
    protected void onMessage(EventMessage message) {}

    /**
     * Override to process metadata messages
     * 
     * @param message
     *            metadata
     */
    protected void onMessage(MetadataMessage message) {}

    /**
     * @return computation start time in milliseconds since midnight, January 1, 1970 UTC
     */
    public Long getStartTimeMs() {
        return this.startTimeMs;
    }

    /**
     * @return computation stop time in milliseconds since midnight, January 1, 1970 UTC
     */
    public Long getStopTimeMs() {
        return this.stopTimeMs;
    }

    /**
     * Processes the computation
     * 
     * @return computation instance that was processed
     * @throws ComputationAbortedException
     *             Exception thrown if the computation is aborted during its execution
     * @throws ComputationFailedException
     *             Exception thrown when the computation failed after being started
     * @throws SignalFlowException
     *             A generic error encountered when interacting with the SignalFx SignalFlow API
     * @throws IllegalStateException
     *             Exception thrown is computation is closed
     */
    public Computation process() throws ComputationAbortedException, ComputationFailedException,
            SignalFlowException, IllegalStateException {

        if (this.computation.getState() == State.STATE_COMPLETED) {
            throw new IllegalStateException("computation is completed");
        }

        this.startTimeMs = System.currentTimeMillis();
        this.stopTimeMs = null;

        try {
            // iterate computation messages and route to message handling methods
            for (ChannelMessage message : computation) {

                switch (message.getType()) {

                case JOB_START:
                    JobStartMessage jobStartMessage = (JobStartMessage) message;
                    onMessage(jobStartMessage);
                    break;

                case JOB_PROGRESS:
                    JobProgressMessage jobProgressMessage = (JobProgressMessage) message;
                    onMessage(jobProgressMessage);
                    break;

                case DATA_MESSAGE:
                    DataMessage dataMessage = (DataMessage) message;
                    onMessage(dataMessage);
                    break;

                case EVENT_MESSAGE:
                    EventMessage eventMessage = (EventMessage) message;
                    onMessage(eventMessage);
                    break;

                case METADATA_MESSAGE:
                    MetadataMessage metadataMessage = (MetadataMessage) message;
                    onMessage(metadataMessage);
                    break;

                default:
                    break;
                }
            }
        } finally {
            this.stopTimeMs = System.currentTimeMillis();
            close();
        }

        return this.computation;
    }

    /**
     * closes the computation
     */
    public void close() {
        this.computation.close();
    }

    /**
     * Callable implementation that calls process.
     */
    @Override
    public Computation call() throws ComputationAbortedException, ComputationFailedException,
            SignalFlowException, IllegalStateException {
        return process();
    }
}
