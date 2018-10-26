/*
 * Copyright (C) 2016-2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * SignalFx SignalFlow client.
 *
 * Client for SignalFx's SignalFlow real-time analytics API. Allows for the execution of ad-hoc
 * computations, returning its output in real-time as it is produced; to start new background
 * computations; attach, keep alive or stop existing computations.
 *
 * @author dgriff
 */
public class SignalFlowClient {

    private SignalFlowTransport transport;

    /**
     * Client Constructor that uses default transport/settings
     *
     * @param token
     *            user api token
     */
    public SignalFlowClient(String token) {
        this(new WebSocketTransport.TransportBuilder(token).build());
    }

    /**
     * Client Constructor that uses custom transport
     *
     * @param transport
     *            custom created transport
     */
    public SignalFlowClient(SignalFlowTransport transport) {
        this.transport = transport;
    }

    /**
     * Execute the given SignalFlow program and stream the output back.
     *
     * @param program
     *            computation written in signalflow language
     * @return computation instance
     */
    public Computation execute(String program) {
        return new Computation(this.transport, program, Collections.<String, String> emptyMap(),
                false);
    }

    /**
     * This method is deprecated and will be removed in the next major release. Use
     * {@link #execute(String, Long, Long, Long, Long, Boolean, Boolean)} instead
     * 
     * Execute the given SignalFlow program with parameters and stream the output back.
     *
     * @param program
     *            computation written in signalflow language
     * @param start
     *            Optional millisecond start timestamp
     * @param stop
     *            Optional millisecond stop timestamp
     * @param resolution
     *            Optional desired data resolution, in milliseconds
     * @param maxDelay
     *            Optional desired maximum data delay, in milliseconds
     * @param persistent
     *            Optional persistent setting
     * @return computation instance
     */
    @Deprecated
    public Computation execute(String program, long start, long stop, long resolution,
                               long maxDelay, boolean persistent) {
        return execute(program, start, stop, resolution, maxDelay, persistent, false);
    }

    /**
     * Execute the given SignalFlow program with parameters and stream the output back.
     *
     * @param program
     *            computation written in signalflow language
     * @param start
     *            Optional timestamp in milliseconds since epoch. Defaults to the current timestamp.
     * @param stop
     *            Optional timestamp in milliseconds since epoch. Defaults to infinity.
     * @param resolution
     *            Optional the minimum desired data resolution, in milliseconds. This allows the
     *            client to put an upper bound on the number of datapoints in the computation
     *            output.
     * @param maxDelay
     *            Optional desired maximum data delay, in milliseconds between 1 and 900000. When
     *            set to zero or unset, max delay will be evaluated dynamically based on the
     *            historical lag information of the input data.
     * @param persistent
     *            Optional persistent setting
     * @param immediate
     *            Optional adjusts the stop timestamp so that the computation doesn't wait for
     *            future data to be available
     * @return computation instance
     */
    public Computation execute(String program, Long start, Long stop, Long resolution,
                               Long maxDelay, Boolean persistent, Boolean immediate) {
        Map<String, String> params = buildParams("start", start, "stop", stop, "resolution",
                resolution, "maxDelay", maxDelay, "persistent", persistent, "immediate", immediate);
        return new Computation(this.transport, program, params, false);
    }

    /**
     * Start executing the given SignalFlow program without being attached to the output of the
     * computation.
     *
     * @param program
     *            computation written in signalflow language
     */
    public void start(String program) {
        this.transport.start(program, Collections.<String, String> emptyMap());
    }

    /**
     * Start executing the given SignalFlow program without being attached to the output of the
     * computation.
     *
     * @param program
     *            computation written in signalflow language
     * @param start
     *            Optional millisecond start timestamp
     * @param stop
     *            Optional millisecond stop timestamp
     * @param resolution
     *            Optional desired data resolution, in milliseconds
     * @param maxDelay
     *            Optional desired maximum data delay, in milliseconds
     */
    public void start(String program, long start, long stop, long resolution, long maxDelay) {
        Map<String, String> params = buildParams("start", start, "stop", stop, "resolution",
                resolution, "maxDelay", maxDelay);
        this.transport.start(program, params);
    }

    /**
     * Stop a SignalFlow computation
     *
     * @param computation
     *            computation instance
     * @param reason
     *            Optional description of why stop was called
     */
    public void stop(Computation computation, String reason) {
        stop(computation.getId(), reason);
        computation.close();
    }

    /**
     * Stop a SignalFlow computation
     *
     * @param handle
     *            computation id
     * @param reason
     *            Optional description of why stop was called
     */
    public void stop(String handle, String reason) {
        Map<String, String> params = buildParams("reason", reason);
        this.transport.stop(handle, params);
    }

    /**
     * Keepalive a SignalFlow computation.
     *
     * @param handle
     *            computation id
     */
    public void keepalive(String handle) {
        this.transport.keepalive(handle);
    }

    /**
     * Attach to an existing SignalFlow computation.
     *
     * @param handle
     *            computation id
     * @param filters
     *            filter written in signalflow language
     * @param resolution
     *            Optional desired data resolution, in milliseconds
     * @return computation instance
     */
    public Computation attach(String handle, String filters, long resolution) {
        return new Computation(this.transport, handle,
                buildParams("filters", filters, "resolution", resolution), true);
    }

    /**
     * Close this SignalFlow client.
     */
    public void close() {
        this.transport.close(1000, null);
    }

    private static Map<String, String> buildParams(Object... params) {
        Preconditions.checkArgument(params.length % 2 == 0);
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<String, String>();
        for (int i = 0; i < params.length; i += 2) {
            if (params[i] != null && params[i + 1] != null) {
                builder.put(params[i].toString(), params[i + 1].toString());
            }
        }
        return builder.build();
    }
}
