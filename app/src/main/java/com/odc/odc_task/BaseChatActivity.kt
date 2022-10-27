package com.odc.odc_task

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.odc.odc_task.utils.BUFFER_SIZE_IN_BYTES
import com.odc.odc_task.utils.FROM_HOST_ACTIVITY
import com.odc.odc_task.utils.TYPE
import com.odc.odc_task.utils.USB_TIMEOUT_IN_MS
import java.util.concurrent.atomic.AtomicBoolean


class BaseChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_base_chat)
    }
    private var communicator: AccessoryCommunicator? = null

    private val keepThreadAlive = AtomicBoolean(true)
    private val sendBuffer: MutableList<String> = ArrayList()

    var flag:Boolean? = null
    private val contentTextView: TextView by lazy {
        findViewById(R.id.content_text)
    }

    private val input: EditText by lazy {
        findViewById(R.id.input_edittext)
    }

    fun sendString(string: String?){
        if(flag == FROM_HOST_ACTIVITY) {
            sendBuffer.add(string!!)
        }else{
            communicator!!.send(string?.toByteArray())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_chat)

        val send = findViewById<Button>(R.id.send_button)
        flag = intent.getBooleanExtra(TYPE , false)

        if(flag == FROM_HOST_ACTIVITY){
            Thread(CommunicationRunnable()).start()
        }else{
            accessoryCommunication()
        }
        send.setOnClickListener{
            val inputString = input.text.toString()
            if (!inputString.isEmpty()) {
                sendString(inputString)
                printLineToUI(getString(R.string.local_prompt) + inputString)
                input.setText("")
            }
        }
    }

    private fun accessoryCommunication() {
        communicator = object : AccessoryCommunicator(this) {
            override fun onReceive(payload: ByteArray?, length: Int) {
                printLineToUI("Device> " + String(payload!!, 0, length))
            }

            override fun onError(msg: String?) {
                printLineToUI("error: $msg ,")
            }


            override fun onConnected() {
                printLineToUI("connected")
            }

            override fun onDisconnected() {
                printLineToUI("disconnected")
            }
        }
    }

    protected fun printLineToUI(line: String) {
        runOnUiThread {
            contentTextView.text = """
     ${contentTextView.text}
     $line
     """.trimIndent()
        }
    }

    private inner class CommunicationRunnable : Runnable {
        override fun run() {
            val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
            val device: UsbDevice? = intent.getParcelableExtra(HostConnectionActivity.DEVICE_EXTRA_KEY)
            var endpointIn: UsbEndpoint? = null
            var endpointOut: UsbEndpoint? = null
            val usbInterface = device?.getInterface(0)
            for (i in 0 until device?.getInterface(0)!!.endpointCount) {
                val endpoint = device.getInterface(0).getEndpoint(i)
                if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                    endpointIn = endpoint
                }
                if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                    endpointOut = endpoint
                }
            }
            if (endpointIn == null) {
                printLineToUI("Input Endpoint not found")
                return
            }
            if (endpointOut == null) {
                printLineToUI("Output Endpoint not found")
                return
            }
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                printLineToUI("Could not open device")
                return
            }
            val claimResult = connection.claimInterface(usbInterface, true)
            if (!claimResult) {
                printLineToUI("Could not claim device")
            } else {
                val buff = ByteArray(BUFFER_SIZE_IN_BYTES)
                printLineToUI("Claimed interface - ready to communicate")
                while (keepThreadAlive.get()) {
                    val bytesTransferred = connection.bulkTransfer(
                        endpointIn,
                        buff,
                        buff.size,
                        USB_TIMEOUT_IN_MS
                    )
                    if (bytesTransferred > 0) {
                        printLineToUI("device> " + String(buff, 0, bytesTransferred))
                    }
                    synchronized(sendBuffer) {
                        if (sendBuffer.size > 0) {
                            val sendBuff =
                                sendBuffer[0].toByteArray()
                            connection.bulkTransfer(
                                endpointOut,
                                sendBuff,
                                sendBuff.size,
                                USB_TIMEOUT_IN_MS
                            )
                            sendBuffer.removeAt(0)
                        }
                    }
                }
            }
            connection.releaseInterface(usbInterface)
            connection.close()
        }
    }

    override fun onStop() {
        super.onStop()
        keepThreadAlive.set(false)
    }


}
