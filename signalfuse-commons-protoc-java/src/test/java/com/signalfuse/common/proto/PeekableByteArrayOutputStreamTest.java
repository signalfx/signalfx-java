package com.signalfx.common.proto;

import java.util.Arrays;
import org.junit.Test;
import com.google.common.base.Preconditions;

/**
 * Date: 8/1/14
 * Time: 11:36 AM
 *
 * @author jack
 */
public class PeekableByteArrayOutputStreamTest {

    @Test
    public void testBasic() {
        PeekableByteArrayOutputStream bout = new PeekableByteArrayOutputStream(100);
        Preconditions.checkArgument(bout.read() == -1);
        Preconditions.checkArgument(bout.available() == 0);
        bout.write(1);
        bout.write(2);
        bout.write(3);
        Preconditions.checkArgument(bout.available() == 3);
        Preconditions.checkArgument(bout.read() == 1);
        Preconditions.checkArgument(bout.read() == 2);
        Preconditions.checkArgument(bout.read() == 3);
        Preconditions.checkArgument(bout.read() == -1);
        bout.write(4);
        bout.write(5);
        Preconditions.checkArgument(bout.available() == 2);
        bout.write(6);
        byte[] readInto = new byte[3];
        Preconditions.checkArgument(1 == bout.read(readInto, 1, 1));
        Preconditions.checkArgument(Arrays.equals(readInto, new byte[] { 0, 4, 0 }));
        Preconditions.checkArgument(bout.available() == 2);
        readInto = new byte[3];
        Preconditions.checkArgument(2 == bout.read(readInto, 0, readInto.length));
        Preconditions.checkArgument(Arrays.equals(readInto, new byte[] { 5, 6, 0 }));
        Preconditions.checkArgument(bout.available() == 0);
    }
}
