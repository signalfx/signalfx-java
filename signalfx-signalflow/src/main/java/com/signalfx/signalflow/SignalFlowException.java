/*
 * Copyright (C) 2016-2018 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

/**
 * A generic error encountered when interacting with the SignalFx SignalFlow API.
 *
 * @author dgriff
 */
public class SignalFlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected int code = 0;

    public SignalFlowException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SignalFlowException(String message) {
        super(message);
    }

    public SignalFlowException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignalFlowException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
