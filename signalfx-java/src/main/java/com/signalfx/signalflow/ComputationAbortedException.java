/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.Map;

/**
 * Exception thrown if the computation is aborted during its execution.
 *
 * @author dgriff
 */
public class ComputationAbortedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected String state;
    protected String reason;

    public ComputationAbortedException(Map<String, String> abortInfo) {
        this(abortInfo.get("sf_job_abortState"), abortInfo.get("sf_job_abortReason"));
    }

    private ComputationAbortedException(String state, String reason) {
        super("Computation " + state + ": " + reason);
        this.state = state;
        this.reason = reason;
    }

    public String getState() {
        return this.state;
    }

    public String getReason() {
        return this.reason;
    }
}
