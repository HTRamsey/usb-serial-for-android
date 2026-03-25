package com.hoho.android.usbserial.util;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream wrapper around a {@link UsbSerialPort} for use with protocol
 * libraries that expect standard Java streams.
 */
public class SerialOutputStream extends OutputStream {

    private final UsbSerialPort mPort;
    private int mTimeout;

    public SerialOutputStream(UsbSerialPort port) {
        this(port, 0);
    }

    public SerialOutputStream(UsbSerialPort port, int timeout) {
        if (port == null) throw new IllegalArgumentException("port is null");
        mPort = port;
        mTimeout = timeout;
    }

    public void setTimeout(int timeout) {
        mTimeout = timeout;
    }

    public int getTimeout() {
        return mTimeout;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
        if (len == 0) return;
        if (off == 0) {
            mPort.write(b, len, mTimeout);
            return;
        }
        byte[] buf = new byte[len];
        System.arraycopy(b, off, buf, 0, len);
        mPort.write(buf, len, mTimeout);
    }

    @Override
    public void close() throws IOException {
        mPort.close();
    }
}
