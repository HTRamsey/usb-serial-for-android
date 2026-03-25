/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class UsbSerialProber {

    private final ProbeTable mProbeTable;

    public UsbSerialProber(ProbeTable probeTable) {
        mProbeTable = probeTable;
    }

    public static UsbSerialProber getDefaultProber() {
        return new UsbSerialProber(getDefaultProbeTable());
    }

    public static ProbeTable getDefaultProbeTable() {
        final ProbeTable probeTable = new ProbeTable();
        probeTable.addDriver(CdcAcmSerialDriver.FACTORY);
        probeTable.addDriver(Cp21xxSerialDriver.FACTORY);
        probeTable.addDriver(FtdiSerialDriver.FACTORY);
        probeTable.addDriver(ProlificSerialDriver.FACTORY);
        probeTable.addDriver(Ch34xSerialDriver.FACTORY);
        probeTable.addDriver(GsmModemSerialDriver.FACTORY);
        probeTable.addDriver(ChromeCcdSerialDriver.FACTORY);
        return probeTable;
    }

    /**
     * Finds and builds all possible {@link UsbSerialDriver UsbSerialDrivers}
     * from the currently-attached {@link UsbDevice} hierarchy. This method does
     * not require permission from the Android USB system, since it does not
     * open any of the devices.
     *
     * @param usbManager usb manager
     * @return a list, possibly empty, of all compatible drivers
     */
    public List<UsbSerialDriver> findAllDrivers(final UsbManager usbManager) {
        final List<UsbSerialDriver> result = new ArrayList<>();

        for (final UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            final UsbSerialDriver driver = probeDevice(usbDevice);
            if (driver != null) {
                result.add(driver);
            }
        }
        return result;
    }

    /**
     * Probes a single device for a compatible driver.
     *
     * @param usbDevice the usb device to probe
     * @return a new {@link UsbSerialDriver} compatible with this device, or
     *         {@code null} if none available.
     */
    public UsbSerialDriver probeDevice(final UsbDevice usbDevice) {
        final UsbSerialDriver.Factory factory = mProbeTable.findDriverFactory(usbDevice);
        if (factory != null) {
            return factory.create(usbDevice);
        }
        return null;
    }

}
