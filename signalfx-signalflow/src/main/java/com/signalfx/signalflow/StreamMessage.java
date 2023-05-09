/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * Base class for stream messages received from a SignalFlow computation
 *
 * @author dgriff
 */
public class StreamMessage {

    /**
     * Enumeration of kinds of stream messages
     */
    public static enum Kind {

        CONTROL("control-message",(byte) 1),
        INFORMATION("message",(byte) 2),
        EVENT("event",(byte) 3),
        METADATA("metadata",(byte) 4),
        DATA("data",(byte) 5),
        ERROR("error",(byte) 6),
        EXPIRED_TSID("expired-tsid",(byte) 10);

        private final String specName;
        private final byte type;

        Kind(String specName, byte type) {
            this.specName = specName;
            this.type = type;
        }

        public byte getBinaryType() {
            return type;
        }

        public String toString() {
            return this.specName;
        }

        private static final Map<String, Kind> SPECNAME_KINDS = new HashMap<String, Kind>();
        private static final Map<Integer, Kind> BINARYTYPE_KINDS = new HashMap<Integer, Kind>();
        static {
            for (Kind kind : Kind.values()) {
                SPECNAME_KINDS.put(kind.specName, kind);
                BINARYTYPE_KINDS.put(new Integer(kind.getBinaryType()), kind);
            }
        }

        public static Kind fromSpecName(String specName) {
            Kind kind = SPECNAME_KINDS.get(specName);
            Preconditions.checkArgument(kind != null);
            return kind;
        }

        public static Kind fromBinaryType(int binaryType) {
            Kind kind = BINARYTYPE_KINDS.get(binaryType);
            Preconditions.checkArgument(kind != null);
            return kind;
        }
    };

    private String event;
    private String id;
    private String data;
    private Kind kind;

    public StreamMessage() {
        this.event = "message";
        kind = Kind.INFORMATION;
    }

    public StreamMessage(String event, String id, String data) {
        this.event = event;
        this.id = id;
        this.data = data;

        try {
            this.kind = Kind.fromSpecName(event);
        } catch (IllegalArgumentException ex) {
            kind = Kind.INFORMATION; // set as default kind
        }
    }

    public Kind getKind() {
        return this.kind;
    }

    public boolean isKind(Kind kind) {
        return this.kind == kind;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(event);
        builder.append(":");
        builder.append(id);
        builder.append(":");
        builder.append(data);
        return builder.toString();
    }
}
