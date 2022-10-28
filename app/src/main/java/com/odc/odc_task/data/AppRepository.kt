package com.odc.odc_task.data

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.widget.TextView
import com.odc.odc_task.utils.USB_TIMEOUT_IN_MS

class AppRepository( val context: Context ) {

    fun getUsbManager():UsbManager?{
        return context.getSystemService(Context.USB_SERVICE) as UsbManager?
    }

    fun initAccessory(device: UsbDevice , mUsbManager:UsbManager): Boolean {
        val connection = mUsbManager.openDevice(device) ?: return false
        initStringControlTransfer(connection, 0, "quandoo") // MANUFACTURER
        initStringControlTransfer(connection, 1, "Android2AndroidAccessory") // MODEL
        initStringControlTransfer(
            connection,
            2,
            "showcasing android2android USB communication"
        ) // DESCRIPTION
        initStringControlTransfer(connection, 3, "0.1") // VERSION
        initStringControlTransfer(connection, 4, "http://quandoo.de") // URI
        initStringControlTransfer(connection, 5, "42") // SERIAL
        connection.controlTransfer(0x40, 53, 0, 0, byteArrayOf(), 0, USB_TIMEOUT_IN_MS)
        connection.close()
        return true
    }

    fun searchForUsbAccessory(deviceList: HashMap<String, UsbDevice>): Boolean {
        for (device in deviceList.values) {
            if (isUsbAccessory(device)) {
                return true
            }
        }
        return false
    }

    private fun isUsbAccessory(device: UsbDevice): Boolean {
        return device.productId == 0x2d00 || device.productId == 0x2d01
    }

    private fun initStringControlTransfer(
        deviceConnection: UsbDeviceConnection,
        index: Int,
        string: String
    ) {
        deviceConnection.controlTransfer(
            0x40,
            52,
            0,
            index,
            string.toByteArray(),
            string.length,
            USB_TIMEOUT_IN_MS
        )
    }
}