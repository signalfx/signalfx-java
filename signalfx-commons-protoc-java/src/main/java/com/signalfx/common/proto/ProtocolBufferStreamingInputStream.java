package com.signalfx.common.proto;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import com.github.os72.protobuf351.MessageLite;

/**
 * The idea with this class is that we can encapsulate a collection of protocol buffers and send
 * them to a stream over HTTP without having to store the entire stream as a byte array in memory.
 */
public final class ProtocolBufferStreamingInputStream<ProtocolBufferObject extends MessageLite>
        extends InputStream {

    public static final int DEFAULT_STREAM_SIZE = 1024;
    private final Iterator<ProtocolBufferObject> protoBufferIterator;
    private final PeekableByteArrayOutputStream currentBytes;

    public ProtocolBufferStreamingInputStream(
            Iterator<ProtocolBufferObject> protoBufferIterator) {
        this.protoBufferIterator = protoBufferIterator;
        this.currentBytes = new PeekableByteArrayOutputStream(DEFAULT_STREAM_SIZE);
    }

    /**
     * Fill in our byte buffer if we're out of space by reading the next protocol buffer object.
     *
     * @throws IOException
     *         If {@link MessageLite#writeDelimitedTo(java.io.OutputStream)}
     *         fails
     */
    private void fillBytes() throws IOException {
        if (currentBytes.available() > 0) {
            return;
        }
        currentBytes.reset();
        while (protoBufferIterator.hasNext() && currentBytes.size() <= 1000) {
            protoBufferIterator.next().writeDelimitedTo(currentBytes);
        }
    }

    @Override
    public int read() throws IOException {
        fillBytes();
        return currentBytes.read();
    }

    @Override
    public int available() {
        return currentBytes.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        currentBytes.close();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int total_read = 0;
        while (len > 0) {
            fillBytes();
            int result = currentBytes.read(b, off, len);
            if (result == -1) {
                return total_read == 0 ? -1 : total_read;
            }
            len -= result;
            total_read += result;
            off += result;
        }
        return total_read;
    }
}
