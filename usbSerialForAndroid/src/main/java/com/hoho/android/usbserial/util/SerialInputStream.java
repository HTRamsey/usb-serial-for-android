package com.hoho.android.usbserial.util;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper around a {@link UsbSerialPort} for use with protocol
 * libraries that expect standard Java streams.
 */
public class SerialInputStream extends InputStream {

    private final UsbSerialPort mPort;
    private int mTimeout;

    public SerialInputStream(UsbSerialPort port) {
        this(port, 0);
    }

    public SerialInputStream(UsbSerialPort port, int timeout) {
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
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int n = mPort.read(buf, mTimeout);
        return n > 0 ? buf[0] & 0xFF : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;
        if (off == 0) {
            return mPort.read(b, len, mTimeout);
        }
        byte[] buf = new byte[len];
        int n = mPort.read(buf, len, mTimeout);
        if (n > 0) {
            System.arraycopy(buf, 0, b, off, n);
        }
        return n;
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public void close() throws IOException {
        mPort.close();
    }
}
