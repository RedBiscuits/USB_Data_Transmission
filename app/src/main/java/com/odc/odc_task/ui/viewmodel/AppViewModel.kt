package com.odc.odc_task.ui.viewmodel

import android.hardware.usb.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odc.odc_task.AccessoryCommunicator
import com.odc.odc_task.R
import com.odc.odc_task.data.AppRepository
import com.odc.odc_task.utils.BUFFER_SIZE_IN_BYTES
import com.odc.odc_task.utils.GUEST
import com.odc.odc_task.utils.HOST
import com.odc.odc_task.utils.USB_TIMEOUT_IN_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AppViewModel(
    private val repository: AppRepository
) : ViewModel() {


    private var _type: Boolean? = null
    private var _usbManager: UsbManager? = null
    private var _deviceList: HashMap<String, UsbDevice>? = null
    private var _usbDevice: UsbDevice? = null
    private var _usbEndpointIn: UsbEndpoint? = null
    private var _usbEndpointOut: UsbEndpoint? = null
    private var _usbInterface: UsbInterface? = null
    private val keepThreadAlive = AtomicBoolean(true)
    private val sendBuffer: MutableList<String> = ArrayList()
    private var communicator: AccessoryCommunicator? = null
    var text = MutableLiveData<String>()

    init {
        _usbManager = repository.getUsbManager()
        _deviceList = _usbManager?.deviceList
        if (_deviceList?.values == null || _deviceList?.values!!.isEmpty()) {
            _type = GUEST
            initAccessoryCommunication()
        }
        if (_type != GUEST) {
            initConnectedDevice()
            _type = repository.searchForUsbAccessory(_deviceList!!)
            _usbDevice = _deviceList?.values?.first()
            initHostCommunication()
        }
        text.postValue(" ")
    }

    private fun initConnectedDevice() {
        try {
            for (device in _deviceList!!.values) {
                repository.initAccessory(device, _usbManager!!)
            }
        } catch (err: Exception) {
            updateText(err.printStackTrace().toString())
        }
    }


    fun sendString(inputString: String) {
        if (_type == HOST) {
            sendBuffer.add(inputString)
        } else {
            communicator?.send(inputString.toByteArray())
        }
        updateText(repository.context.getString(R.string.local_prompt) + inputString)
    }

    private fun initHostCommunication() {
        _usbInterface = _usbDevice?.getInterface(0)
        try {
            for (i in 0 until _usbDevice?.getInterface(0)!!.endpointCount) {
                val endpoint = _usbDevice!!.getInterface(0).getEndpoint(i)
                if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                    _usbEndpointIn = endpoint
                }
                if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                    _usbEndpointOut = endpoint
                }
            }
        }catch (err:Exception){
         updateText(err.printStackTrace().toString())
        }
        if (_usbEndpointIn == null) {
            updateText("Input Endpoint not found")
            return
        }
        if (_usbEndpointOut == null) {
            updateText("Output Endpoint not found")
            return
        }
        val connection = _usbManager?.openDevice(_usbDevice)
        if (connection == null) {
            updateText("Could not open device")
            return
        }
        val claimResult = connection.claimInterface(_usbInterface, true)
        if (!claimResult) {
            updateText("Could not claim device")
        } else {
            val buff = ByteArray(BUFFER_SIZE_IN_BYTES)
            updateText("Claimed interface - ready to communicate")
            while (keepThreadAlive.get()) {
                val bytesTransferred = connection.bulkTransfer(
                    _usbEndpointIn,
                    buff,
                    buff.size,
                    USB_TIMEOUT_IN_MS
                )
                if (bytesTransferred > 0) {
                    updateText("device> " + String(buff, 0, bytesTransferred))
                }
                synchronized(sendBuffer) {
                    if (sendBuffer.size > 0) {
                        val sendBuff =
                            sendBuffer[0].toByteArray()
                        connection.bulkTransfer(
                            _usbEndpointOut,
                            sendBuff,
                            sendBuff.size,
                            USB_TIMEOUT_IN_MS
                        )
                        sendBuffer.removeAt(0)
                    }
                }
            }
        }
        connection.releaseInterface(_usbInterface)
        connection.close()
    }

    private fun initAccessoryCommunication() {
        communicator = object : AccessoryCommunicator(repository.context) {
            override fun onReceive(payload: ByteArray?, length: Int) {
                updateText("Device> " + String(payload!!, 0, length))
            }

            override fun onError(msg: String?) {
                updateText("error: $msg ,")
            }


            override fun onConnected() {
                updateText("connected")
            }

            override fun onDisconnected() {
                updateText("disconnected")
            }
        }
    }

    private fun updateText(line: String) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            text.value += line + "\n"

//            = """
//     ${text.value}
//     $line
//     """.trimIndent()
        }
    }

    override fun onCleared() {
        super.onCleared()
        keepThreadAlive.set(false)
    }
}