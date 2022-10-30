package com.odc.odc_task

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.odc.odc_task.utils.*


class HostConnectionActivity : AppCompatActivity() {

    // Connection manager
    private var mUsbManager: UsbManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_chat)

        // Referencing USB devices
        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager?
        val deviceList = mUsbManager?.deviceList

        // If no devices attached then it's an accessory
        // launches activity as a guest/accessory
        if (deviceList == null || deviceList.size == 0) {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra(TYPE , GUEST_ACTIVITY)
            startActivity(intent)
            finish()
            return
        }

        /* If not an accessory then it's a host */

        // Initializing all accessories for communication
        for (device in deviceList.values) {
            initAccessory(device)
        }

        // Starting activity as a host
        if (searchForUsbAccessory(deviceList)) {
            return
        }

        finish()
    }

    // Starts activity whenever an attached device exists
    private fun searchForUsbAccessory(deviceList: HashMap<String, UsbDevice>): Boolean {
        for (device in deviceList.values) {
            if (isUsbAccessory(device)) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra(DEVICE_EXTRA_KEY, device)
                intent.putExtra(TYPE , HOST_ACTIVITY)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }

    // Filters the first device
    // can be changed at the XML usb-device filter
    private fun isUsbAccessory(device: UsbDevice): Boolean {
        return device.productId == 0x2d00 || device.productId == 0x2d01
    }

    // Forcing accessory
    // Taken from StackOverFlow.com
    private fun initAccessory(device: UsbDevice): Boolean {
        val connection = mUsbManager!!.openDevice(device) ?: return false
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

    // Also taken from StackOverFlow.com
    // Simplifies the accessory forcing code
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
