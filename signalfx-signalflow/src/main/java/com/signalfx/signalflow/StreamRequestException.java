/*
 * Copyright (C) 2019 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.List;

/**
 * Exception thrown when the computation fails at request time, possibly for syntax errors.
 *
 * @author cwatson
 */
public class StreamRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected int errorCode;
    protected String message;

    public StreamRequestException(int errorCode, String message) {
        super("Computation failed (" + message + ") code: " + errorCode);
        this.message = message;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getMessage() {
        return this.message;
    }
}
