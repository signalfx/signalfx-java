package com.signalfx.common.proto;

import java.io.ByteArrayOutputStream;
import com.google.common.base.Preconditions;

/**
 * <p>A stream that users can output bytes to as well as peek at and read bytes from.  It's trying to
 * be both an {@link java.io.InputStream} and {@link java.io.OutputStream}, but can't actually be
 * both because of multiple inheritance.</p>
 *
 * <p>If you call {@link #reset()} frequently when the stream is empty, you can stream a large amount
 * of data without using much memory at all.</p>
 */
public final class PeekableByteArrayOutputStream extends ByteArrayOutputStream {
    private int position;

    public PeekableByteArrayOutputStream(int size) {
        super(size);
        position = 0;
    }

    /**
     * How many bytes are available for reading currently from this stream.
     *
     * @return Readable bytes
     */
    public int available() {
        return size() - position;
    }

    /**
     * Similar to {@link java.io.InputStream#read()}
     * @return Next byte, or -1
     */
    public int read() {
        if (available() == 0) {
            return -1;
        }
        final byte to_return = buf[position++];
        if (available() == 0) {
            reset();
        }
        return to_return;
    }

    /**
     * Similar to {@link java.io.InputStream#read(byte[], int, int)}
     * @param readInput    Array to read into
     * @param off          Offset to read into
     * @param len          Number of bytes to attempt to read
     * @return Number of bytes read
     */
    public int read(byte[] readInput, int off, int len) {
        if (count == position) {
            return -1;
        }
        final int limit_to_read = Math.min(len, count - position);
        System.arraycopy(buf, position, readInput, off, limit_to_read);
        position += limit_to_read;
        if (available() == 0) {
            reset();
        }
        return limit_to_read;
    }

    @Override public void reset() {
        super.reset();
        position = 0;
        Preconditions.checkArgument(count == 0, "I expect count to reset");
    }
}
