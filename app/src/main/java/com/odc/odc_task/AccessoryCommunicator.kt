package com.odc.odc_task

import android.content.Context
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Message
import android.os.ParcelFileDescriptor
import com.odc.odc_task.utils.BUFFER_SIZE_IN_BYTES
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

abstract class AccessoryCommunicator(context: Context) {
    // Accessory communicator vars
    private val usbManager: UsbManager
    private var sendHandler: Handler? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inStream: FileInputStream? = null
    private var outStream: FileOutputStream? = null
    private var running = false

    // Guided by an answer on github forums
    fun send(payload: ByteArray?) {
        if (sendHandler != null) {
            val msg = sendHandler!!.obtainMessage()
            msg.obj = payload
            sendHandler!!.sendMessage(msg)
        }
    }

    private fun receive(payload: ByteArray, length: Int) {
        onReceive(payload, length)
    }

    abstract fun onReceive(payload: ByteArray?, length: Int)
    abstract fun onError(msg: String?)
    abstract fun onConnected()
    abstract fun onDisconnected()

    // Multithreaded class to handle msgs and errors at background
    private inner class CommunicationThread : Thread() {
        override fun run() {
            running = true
            while (running) {
                val msg = ByteArray(BUFFER_SIZE_IN_BYTES)
                try {
                    //Handle incoming messages
                    var len = inStream!!.read(msg)
                    while (inStream != null && len > 0 && running) {
                        receive(msg, len)
                        sleep(10)
                        len = inStream!!.read(msg)
                    }
                } catch (e: Exception) {
                    onError("USB Receive Failed $e\n")
                    closeAccessory()
                }
            }
        }
    }

    // Opens connection with accessory
    private fun openAccessory(accessory: UsbAccessory) {
        fileDescriptor = usbManager.openAccessory(accessory)
        if (fileDescriptor != null) {
            val fd = fileDescriptor!!.fileDescriptor
            inStream = FileInputStream(fd)
            outStream = FileOutputStream(fd)
            CommunicationThread().start()
            sendHandler = object : Handler() {
                override fun handleMessage(msg: Message) {
                    try {
                        outStream!!.write(msg.obj as ByteArray)
                    } catch (e: Exception) {
                        onError("USB Send Failed $e\n")
                    }
                }
            }
            onConnected()
        } else {
            onError("could not connect")
        }
    }

    // Terminating connections with accessory to avoid mem leak
    fun closeAccessory() {
        running = false
        try {
            if (fileDescriptor != null) {
                fileDescriptor!!.close()
            }
        } catch (e: IOException) {
        } finally {
            fileDescriptor = null
        }
        onDisconnected()
    }

    // initializing variables
    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val accessoryList = usbManager.accessoryList
        if (accessoryList == null || accessoryList.isEmpty()) {
            onError("no accessory found")
        } else {
            openAccessory(accessoryList[0])
        }
    }
}