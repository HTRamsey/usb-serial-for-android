package com.hoho.android.usbserial.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.hardware.usb.UsbEndpoint;
import android.os.Process;

import com.hoho.android.usbserial.driver.CommonUsbSerialPort;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SerialInputOutputManagerExtendedTest {

    private CommonUsbSerialPort port;
    private SerialInputOutputManager manager;

    @Before
    public void setUp() {
        UsbEndpoint readEndpoint = mock(UsbEndpoint.class);
        when(readEndpoint.getMaxPacketSize()).thenReturn(64);
        port = mock(CommonUsbSerialPort.class);
        when(port.getReadEndpoint()).thenReturn(readEndpoint);
        when(port.isOpen()).thenReturn(true);
        manager = new SerialInputOutputManager(port);
        manager.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
    }

    @Test
    public void stateTransitions() {
        assertEquals(SerialInputOutputManager.State.STOPPED, manager.getState());
    }

    @Test
    public void setReadTimeout() {
        manager.setReadTimeout(500);
        assertEquals(500, manager.getReadTimeout());
    }

    @Test
    public void setWriteTimeout() {
        manager.setWriteTimeout(500);
        assertEquals(500, manager.getWriteTimeout());
    }

    @Test
    public void setReadBufferSize() {
        manager.setReadBufferSize(1024);
        assertEquals(1024, manager.getReadBufferSize());
    }

    @Test
    public void setWriteBufferSize() {
        manager.setWriteBufferSize(8192);
        assertEquals(8192, manager.getWriteBufferSize());
    }

    @Test
    public void writeAsyncExceedsCapacityThrows() {
        manager.setWriteBufferSize(16);
        assertThrows(IllegalArgumentException.class, () -> manager.writeAsync(new byte[32]));
    }

    @Test
    public void writeAsyncWhenNotRunningThrows() {
        manager.setWriteBufferSize(16);
        // Fill the buffer and try to write more — should throw since not running
        manager.writeAsync(new byte[16]);
        assertThrows(IllegalStateException.class, () -> manager.writeAsync(new byte[1]));
    }

    @Test
    public void listenerCallbackOnData() throws Exception {
        when(port.read(any(byte[].class), anyInt())).thenReturn(3).thenThrow(new IOException("done"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> received = new AtomicReference<>();
        manager.setListener(new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                received.set(data);
                latch.countDown();
            }
            @Override
            public void onRunError(Exception e) {}
        });

        manager.runRead();
        assertNotNull(received.get());
        assertEquals(3, received.get().length);
    }

    @Test
    public void errorCallbackOnException() throws Exception {
        when(port.read(any(byte[].class), anyInt())).thenThrow(new IOException("test error"));

        AtomicReference<Exception> error = new AtomicReference<>();
        manager.setListener(new SerialInputOutputManager.Listener() {
            @Override public void onNewData(byte[] data) {}
            @Override public void onRunError(Exception e) { error.set(e); }
        });

        manager.runRead();
        assertNotNull(error.get());
        assertEquals("test error", error.get().getMessage());
    }

    @Test
    public void controlLineListenerPolled() throws Exception {
        when(port.read(any(byte[].class), anyInt()))
                .thenReturn(0)
                .thenReturn(0)
                .thenThrow(new IOException("done"));
        when(port.getControlLines()).thenReturn(java.util.EnumSet.of(
                com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.CTS));

        AtomicReference<java.util.EnumSet<?>> lines = new AtomicReference<>();
        manager.setControlLineListener(controlLines -> lines.set(controlLines), 0);

        manager.runRead();
        assertNotNull(lines.get());
        assertTrue(lines.get().contains(
                com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.CTS));
    }

    @Test
    public void closeable() {
        // Verify Closeable interface works
        manager.close();
        assertEquals(SerialInputOutputManager.State.STOPPED, manager.getState());
    }

    @Test
    public void defaultReadBufferMatchesEndpoint() {
        assertEquals(64, manager.getReadBufferSize());
    }
}
