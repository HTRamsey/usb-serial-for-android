/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;

import java.util.List;
import java.util.Map;

public interface UsbSerialDriver {

    /**
     * Factory for creating driver instances from a USB device.
     */
    interface Factory {
        UsbSerialDriver create(UsbDevice device);
        Map<Integer, int[]> getSupportedDevices();
    }

    /**
     * Optional interface for drivers that can probe USB devices by inspecting
     * their interfaces, beyond simple VID/PID matching.
     */
    interface DeviceProbe {
        boolean probe(UsbDevice device);
    }

    /**
     * Returns the raw {@link UsbDevice} backing this port.
     *
     * @return the device
     */
    UsbDevice getDevice();

    /**
     * Returns all available ports for this device. This list must have at least
     * one entry.
     *
     * @return the ports
     */
    List<UsbSerialPort> getPorts();
}
