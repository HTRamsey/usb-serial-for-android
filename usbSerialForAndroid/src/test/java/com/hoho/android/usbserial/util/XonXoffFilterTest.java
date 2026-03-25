package com.hoho.android.usbserial.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.junit.Test;

public class XonXoffFilterTest {

    @Test
    public void initialState() {
        XonXoffFilter filter = new XonXoffFilter();
        assertTrue(filter.getXON());
    }

    @Test
    public void noControlChars() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] data = {0x41, 0x42, 0x43};
        byte[] result = filter.filter(data);
        assertArrayEquals(data, result);
        assertTrue(filter.getXON());
    }

    @Test
    public void xoffSetsState() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] data = {0x41, UsbSerialPort.CHAR_XOFF, 0x42};
        byte[] result = filter.filter(data);
        assertArrayEquals(new byte[]{0x41, 0x42}, result);
        assertFalse(filter.getXON());
    }

    @Test
    public void xonRestoresState() {
        XonXoffFilter filter = new XonXoffFilter();
        filter.filter(new byte[]{UsbSerialPort.CHAR_XOFF});
        assertFalse(filter.getXON());

        byte[] result = filter.filter(new byte[]{0x41, UsbSerialPort.CHAR_XON, 0x42});
        assertArrayEquals(new byte[]{0x41, 0x42}, result);
        assertTrue(filter.getXON());
    }

    @Test
    public void multipleControlChars() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] data = {UsbSerialPort.CHAR_XON, 0x41, UsbSerialPort.CHAR_XOFF, UsbSerialPort.CHAR_XON, 0x42};
        byte[] result = filter.filter(data);
        assertArrayEquals(new byte[]{0x41, 0x42}, result);
        assertTrue(filter.getXON());
    }

    @Test
    public void onlyControlChars() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] data = {UsbSerialPort.CHAR_XOFF};
        byte[] result = filter.filter(data);
        assertArrayEquals(new byte[0], result);
        assertFalse(filter.getXON());
    }

    @Test
    public void emptyData() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] result = filter.filter(new byte[0]);
        assertArrayEquals(new byte[0], result);
        assertTrue(filter.getXON());
    }

    @Test
    public void lastControlCharWins() {
        XonXoffFilter filter = new XonXoffFilter();
        filter.filter(new byte[]{UsbSerialPort.CHAR_XOFF, UsbSerialPort.CHAR_XON, UsbSerialPort.CHAR_XOFF});
        assertFalse(filter.getXON());

        filter.filter(new byte[]{UsbSerialPort.CHAR_XON});
        assertTrue(filter.getXON());
    }

    @Test
    public void returnsSameArrayWhenNoFilter() {
        XonXoffFilter filter = new XonXoffFilter();
        byte[] data = {0x01, 0x02, 0x03};
        byte[] result = filter.filter(data);
        // when no control chars found, same array reference is returned
        assertTrue(data == result);
    }
}
