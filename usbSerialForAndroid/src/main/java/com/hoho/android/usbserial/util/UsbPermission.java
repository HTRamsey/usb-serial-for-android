package com.hoho.android.usbserial.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;

/**
 * Helper to request USB device permission and open a connection.
 *
 * <pre>
 * UsbPermission.request(context, device, (connection, granted) -> {
 *     if (granted) {
 *         port.open(connection);
 *     }
 * });
 * </pre>
 */
public class UsbPermission {

    private static final String ACTION_USB_PERMISSION = "com.hoho.android.usbserial.USB_PERMISSION";

    public interface Callback {
        void onResult(UsbDeviceConnection connection, boolean granted);
    }

    /**
     * Request permission for a USB device. If already granted, the callback
     * fires immediately on the calling thread. Otherwise a system dialog is
     * shown and the callback fires on the main thread when the user responds.
     *
     * @param context application or activity context
     * @param device  the USB device
     * @param callback result callback
     */
    public static void request(Context context, UsbDevice device, Callback callback) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            callback.onResult(null, false);
            return;
        }

        if (manager.hasPermission(device)) {
            UsbDeviceConnection connection = manager.openDevice(device);
            callback.onResult(connection, connection != null);
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                context.getApplicationContext().unregisterReceiver(this);
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                UsbDeviceConnection connection = null;
                if (granted) {
                    connection = manager.openDevice(device);
                    granted = connection != null;
                }
                callback.onResult(connection, granted);
            }
        };

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), flags);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getApplicationContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.getApplicationContext().registerReceiver(receiver, filter);
        }
        manager.requestPermission(device, permissionIntent);
    }
}
