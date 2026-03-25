package com.hoho.android.usbserial.driver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CommonUsbSerialPortReadWriteTest {

    static class TestDriver implements UsbSerialDriver {
        ArrayList<UsbSerialPort> ports = new ArrayList<>();
        TestDriver() { ports.add(new TestPort(null, 0)); }
        @Override public UsbDevice getDevice() { return null; }
        @Override public List<UsbSerialPort> getPorts() { return ports; }
    }

    static class TestPort extends CommonUsbSerialPort {
        public TestPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
            mUsbRequestSupplier = TestUsbRequest::new;
        }
        @Override protected void openInt() throws IOException {
            mReadEndpoint = mock(UsbEndpoint.class);
            when(mReadEndpoint.getMaxPacketSize()).thenReturn(64);
            mWriteEndpoint = mock(UsbEndpoint.class);
            when(mWriteEndpoint.getMaxPacketSize()).thenReturn(64);
        }
        @Override protected void closeInt() {}
        @Override public UsbSerialDriver getDriver() { return null; }
        @Override public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {}
    }

    static class TestUsbRequest extends UsbRequest {
        @Override public boolean initialize(UsbDeviceConnection connection, UsbEndpoint endpoint) { return true; }
        @Override public void setClientData(Object data) {}
        @Override public boolean queue(ByteBuffer buffer, int length) { return true; }
    }

    private UsbDeviceConnection connection;
    private TestPort port;

    @Before
    public void setUp() throws Exception {
        connection = mock(UsbDeviceConnection.class);
        when(connection.controlTransfer(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt())).thenReturn(0);
        TestDriver driver = new TestDriver();
        port = (TestPort) driver.getPorts().get(0);
        port.open(connection);
    }

    @Test
    public void readBulkBasic() throws Exception {
        when(connection.bulkTransfer(eq(port.mReadEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(5);
        byte[] buf = new byte[64];
        int n = port.read(buf, 100);
        assertEquals(5, n);
    }

    @Test
    public void readReturnsZeroOnTimeout() throws Exception {
        when(connection.bulkTransfer(eq(port.mReadEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(-1);
        byte[] buf = new byte[64];
        int n = port.read(buf, 100);
        assertEquals(0, n);
    }

    @Test
    public void readEmptyBufferThrows() {
        assertThrows(IllegalArgumentException.class, () -> port.read(new byte[0], 100));
    }

    @Test
    public void readNegativeLengthThrows() {
        assertThrows(IllegalArgumentException.class, () -> port.read(new byte[64], -1, 100));
    }

    @Test
    public void readLengthClampedToBuffer() throws Exception {
        when(connection.bulkTransfer(eq(port.mReadEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(3);
        byte[] buf = new byte[10];
        int n = port.read(buf, 100, 100);
        assertEquals(3, n);
    }

    @Test
    public void writeBasic() throws Exception {
        when(connection.bulkTransfer(eq(port.mWriteEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(5);
        port.write(new byte[]{1, 2, 3, 4, 5}, 100);
    }

    @Test
    public void writeTimesOut() {
        when(connection.bulkTransfer(eq(port.mWriteEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(-1);
        assertThrows(IOException.class, () -> port.write(new byte[]{1, 2, 3}, 1));
    }

    @Test
    public void readByteBufferHeapFastPath() throws Exception {
        when(connection.bulkTransfer(eq(port.mReadEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(3);
        ByteBuffer buf = ByteBuffer.allocate(64);
        int n = port.read(buf, 100);
        assertEquals(3, n);
        assertEquals(3, buf.position());
    }

    @Test
    public void readByteBufferDirect() throws Exception {
        when(connection.bulkTransfer(eq(port.mReadEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(2);
        ByteBuffer buf = ByteBuffer.allocateDirect(64);
        int n = port.read(buf, 100);
        assertEquals(2, n);
        assertEquals(2, buf.position());
    }

    @Test
    public void writeByteBufferHeapFastPath() throws Exception {
        when(connection.bulkTransfer(eq(port.mWriteEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(3);
        ByteBuffer buf = ByteBuffer.wrap(new byte[]{1, 2, 3});
        port.write(buf, 100);
        assertEquals(3, buf.position());
    }

    @Test
    public void writeByteBufferDirect() throws Exception {
        when(connection.bulkTransfer(eq(port.mWriteEndpoint), any(byte[].class), anyInt(), anyInt()))
                .thenReturn(3);
        ByteBuffer buf = ByteBuffer.allocateDirect(3);
        buf.put(new byte[]{1, 2, 3});
        buf.flip();
        port.write(buf, 100);
        assertEquals(3, buf.position());
    }

    @Test
    public void getSerialWhenClosed() throws Exception {
        port.close();
        assertThrows(IllegalStateException.class, () -> port.getSerial());
    }

    @Test
    public void isOpenState() throws Exception {
        assertEquals(true, port.isOpen());
        port.close();
        assertEquals(false, port.isOpen());
    }

    @Test
    public void doubleOpenThrows() {
        assertThrows(IOException.class, () -> port.open(connection));
    }

    @Test
    public void doubleCloseThrows() throws Exception {
        port.close();
        assertThrows(IOException.class, () -> port.close());
    }

    @Test
    public void openWithNullConnectionThrows() throws Exception {
        port.close();
        assertThrows(IllegalArgumentException.class, () -> port.open(null));
    }
}
