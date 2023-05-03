/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.List;

/**
 * Exception thrown when the computation failed after being started.
 *
 * @author dgriff
 */
public class ComputationFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected List<Object> errors;

    public ComputationFailedException(List<Object> errors) {
        super("Computation failed (" + errors + ")");
        this.errors = errors;
    }

    public List<Object> getErrors() {
        return this.errors;
    }
}
