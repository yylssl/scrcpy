package com.genymobile.scrcpy;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class IO {
    private IO() {
        // not instantiable
    }

    public static void writeFullyStream(FileDescriptor fd, byte[] buffer, int offset, int len) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(fd);
        int writeLen = 0;
        while (writeLen < len) {
            try {
                int w = fileOutputStream.write(buffer, offset + writeLen, len - writeLen);
                if (BuildConfig.DEBUG && w < 0) {
                    // w should not be negative, since an exception is thrown on error
                    throw new AssertionError("FileOutputStream.write() returned a negative value (" + w + ")");
                }
                writeLen += w;
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EINTR) {
                    throw new IOException(e);
                }
            }
        }
    }

    public static void writeFully(FileDescriptor fd, ByteBuffer from) throws IOException {
        // ByteBuffer position is not updated as expected by Os.write() on old Android versions, so
        // count the remaining bytes manually.
        // See <https://github.com/Genymobile/scrcpy/issues/291>.
        int remaining = from.remaining();
        while (remaining > 0) {
            try {
                int w = Os.write(fd, from);
                if (BuildConfig.DEBUG && w < 0) {
                    // w should not be negative, since an exception is thrown on error
                    throw new AssertionError("Os.write() returned a negative value (" + w + ")");
                }
                remaining -= w;
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EINTR) {
                    throw new IOException(e);
                }
            }
        }
    }

    public static void writeFully(FileDescriptor fd, byte[] buffer, int offset, int len) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            writeFully(fd, ByteBuffer.wrap(buffer, offset, len));
        } else {
            writeFullyStream(fd, buffer, offset, len);
        }
    }
}
