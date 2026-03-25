/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps (vendor id, product id) pairs to the corresponding serial driver factory,
 * or uses a probe function to check actual USB devices for matching interfaces.
 */
public class ProbeTable {

    private final Map<Pair<Integer, Integer>, UsbSerialDriver.Factory> mVidPidProbeTable =
            new LinkedHashMap<>();
    private final Map<UsbSerialDriver.DeviceProbe, UsbSerialDriver.Factory> mDeviceProbeTable =
            new LinkedHashMap<>();

    /**
     * Adds or updates a (vendor, product) pair in the table.
     *
     * @param vendorId the USB vendor id
     * @param productId the USB product id
     * @param factory the driver factory responsible for this pair
     * @return {@code this}, for chaining
     */
    public ProbeTable addProduct(int vendorId, int productId, UsbSerialDriver.Factory factory) {
        mVidPidProbeTable.put(Pair.create(vendorId, productId), factory);
        return this;
    }

    /**
     * Adds or updates a (vendor, product) pair in the table using a driver class.
     * The class must have a constructor that takes a single {@link UsbDevice} argument.
     * Kept for backward compatibility with custom probers.
     *
     * @param vendorId the USB vendor id
     * @param productId the USB product id
     * @param driverClass the driver class responsible for this pair
     * @return {@code this}, for chaining
     */
    public ProbeTable addProduct(int vendorId, int productId,
            Class<? extends UsbSerialDriver> driverClass) {
        mVidPidProbeTable.put(Pair.create(vendorId, productId), new ReflectiveFactory(driverClass));
        return this;
    }

    /**
     * Registers a driver factory with its supported VID/PID pairs and optional device probe.
     *
     * @param factory the driver factory
     */
    void addDriver(UsbSerialDriver.Factory factory) {
        Map<Integer, int[]> devices = factory.getSupportedDevices();
        for (Map.Entry<Integer, int[]> entry : devices.entrySet()) {
            final int vendorId = entry.getKey();
            for (int productId : entry.getValue()) {
                addProduct(vendorId, productId, factory);
            }
        }
        if (factory instanceof UsbSerialDriver.DeviceProbe) {
            mDeviceProbeTable.put((UsbSerialDriver.DeviceProbe) factory, factory);
        }
    }

    /**
     * Returns the driver class for the given USB device, or {@code null} if no match.
     * For backward compatibility; prefers {@link #findDriverFactory(UsbDevice)}.
     *
     * @param usbDevice the USB device to be probed
     * @return the driver class matching this device, or {@code null}
     */
    public Class<? extends UsbSerialDriver> findDriver(final UsbDevice usbDevice) {
        UsbSerialDriver.Factory factory = findDriverFactory(usbDevice);
        if (factory == null)
            return null;
        return factory.create(usbDevice).getClass();
    }

    /**
     * Returns the driver factory for the given USB device, or {@code null} if no match.
     *
     * @param usbDevice the USB device to be probed
     * @return the driver factory matching this device, or {@code null}
     */
    UsbSerialDriver.Factory findDriverFactory(final UsbDevice usbDevice) {
        final Pair<Integer, Integer> pair = Pair.create(usbDevice.getVendorId(), usbDevice.getProductId());
        UsbSerialDriver.Factory factory = mVidPidProbeTable.get(pair);
        if (factory != null)
            return factory;
        for (Map.Entry<UsbSerialDriver.DeviceProbe, UsbSerialDriver.Factory> entry : mDeviceProbeTable.entrySet()) {
            if (entry.getKey().probe(usbDevice))
                return entry.getValue();
        }
        return null;
    }

    /**
     * Reflective factory for backward compatibility with {@link #addProduct(int, int, Class)}.
     */
    private static class ReflectiveFactory implements UsbSerialDriver.Factory {
        private final Class<? extends UsbSerialDriver> mDriverClass;

        ReflectiveFactory(Class<? extends UsbSerialDriver> driverClass) {
            mDriverClass = driverClass;
        }

        @Override
        public UsbSerialDriver create(UsbDevice device) {
            try {
                return mDriverClass.getConstructor(UsbDevice.class).newInstance(device);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate " + mDriverClass.getName(), e);
            }
        }

        @Override
        public Map<Integer, int[]> getSupportedDevices() {
            return new LinkedHashMap<>();
        }
    }

}
