/*
 * Copyright (C) 2016 SignalFx, Inc. All rights reserved.
 */
package com.signalfx.signalflow;

import java.io.Closeable;
import java.util.Iterator;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract immutable representation for open channels that receive streaming data from a SignalFlow
 * computation.
 *
 * Channel objects bridge the gap between an underlying transport and a higher-level Computation
 * object by providing a transport-agnostic and encoding-agnostic access to the stream of
 * messages.StreamMessage objects that are received for a given computation.
 *
 * Channels are iterable that return ChannelMessage instances.
 *
 * @author dgriff
 */
public abstract class Channel implements Iterator<ChannelMessage>, Closeable {

    protected static final Logger log = LoggerFactory.getLogger(Channel.class);
    private static final int CHANNEL_NAME_LENGTH = 8;

    // unique id for the channel
    protected final String name;

    protected boolean isClosed = false;
    protected Iterator<StreamMessage> iterator;

    protected Channel() {
        this.name = "channel-" + RandomStringUtils.random(CHANNEL_NAME_LENGTH, true, true);
    }

    public Channel(final Iterator<StreamMessage> iterator) {
        this.iterator = iterator;
        this.name = "channel-" + RandomStringUtils.random(CHANNEL_NAME_LENGTH, true, true);
    }

    public String getName() {
        return this.name;
    }

    public boolean hasNext() {
        if (!isClosed()) {
            return this.iterator.hasNext();
        } else {
            throw new IllegalStateException("channel is closed");
        }
    }

    public ChannelMessage next() {
        if (!isClosed()) {
            ChannelMessage message = null;
            while (message == null) {
                StreamMessage streamMessage = this.iterator.next();
                message = ChannelMessage.decodeStreamMessage(streamMessage);
                if (message == null) {
                    log.warn("Unsupported control message {}. ignoring!", streamMessage);
                }
            }
            return message;

        } else {
            throw new IllegalStateException("channel is closed");
        }
    }

    public void remove() {
        if (!isClosed()) {
            this.iterator.remove();
        } else {
            throw new IllegalStateException("channel is closed");
        }
    }

    public void close() {
        this.isClosed = true;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public String toString() {
        return "channel<" + this.name + ">";
    }
}
