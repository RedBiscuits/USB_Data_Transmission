package com.odc.odc_task

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.odc.odc_task.utils.FROM_GUEST_ACTIVITY
import com.odc.odc_task.utils.FROM_HOST_ACTIVITY
import com.odc.odc_task.utils.TYPE
import com.odc.odc_task.utils.USB_TIMEOUT_IN_MS


class HostConnectionActivity : AppCompatActivity() {

    private var mUsbManager: UsbManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_chat)
        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager?

        val deviceList = mUsbManager?.deviceList
        if (deviceList == null || deviceList.size == 0) {
            val intent = Intent(this, BaseChatActivity::class.java)
            intent.putExtra(TYPE , FROM_GUEST_ACTIVITY)
            startActivity(intent)
            finish()
            return
        }

        for (device in deviceList.values) {
            initAccessory(device)
        }

        if (searchForUsbAccessory(deviceList)) {
            return
        }

        finish()
    }

    private fun searchForUsbAccessory(deviceList: HashMap<String, UsbDevice>): Boolean {
        for (device in deviceList.values) {
            if (isUsbAccessory(device)) {
                val intent = Intent(this, BaseChatActivity::class.java)
                intent.putExtra(DEVICE_EXTRA_KEY, device)
                intent.putExtra(TYPE , FROM_HOST_ACTIVITY)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }

    private fun isUsbAccessory(device: UsbDevice): Boolean {
        return device.productId == 0x2d00 || device.productId == 0x2d01
    }

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

    companion object {
        const val DEVICE_EXTRA_KEY = "device"
    }
}
