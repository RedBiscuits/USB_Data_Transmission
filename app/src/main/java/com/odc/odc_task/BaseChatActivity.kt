package com.odc.odc_task

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.odc.odc_task.utils.BUFFER_SIZE_IN_BYTES
import com.odc.odc_task.utils.HOST_ACTIVITY
import com.odc.odc_task.utils.TYPE
import com.odc.odc_task.utils.USB_TIMEOUT_IN_MS
import java.util.concurrent.atomic.AtomicBoolean


class BaseChatActivity : AppCompatActivity() {

    private var gameActive = true
    private var activePlayer = 0
    private var gameState = intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2)
    private var counter: Int = 0
    private var winPositions = arrayOf(
        intArrayOf(0, 1, 2),
        intArrayOf(3, 4, 5),
        intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6),
        intArrayOf(1, 4, 7),
        intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8),
        intArrayOf(2, 4, 6)
    )
    private val images by lazy {
        arrayOf(
            (findViewById<View>(R.id.imageView0) as ImageView),
            (findViewById<View>(R.id.imageView1) as ImageView),
            (findViewById<View>(R.id.imageView2) as ImageView),
            (findViewById<View>(R.id.imageView3) as ImageView),
            (findViewById<View>(R.id.imageView4) as ImageView),
            (findViewById<View>(R.id.imageView5) as ImageView),
            (findViewById<View>(R.id.imageView6) as ImageView),
            (findViewById<View>(R.id.imageView7) as ImageView),
            (findViewById<View>(R.id.imageView8) as ImageView)
        )
    }

    private var communicator: AccessoryCommunicator? = null
    private val keepThreadAlive = AtomicBoolean(true)
    private val sendBuffer: MutableList<String> = ArrayList()
    private var ownerFlag: Boolean? = null
    private val contentTextView: TextView by lazy {
        findViewById(R.id.content_text)
    }
//    private val input: EditText by lazy {
//        findViewById(R.id.input_edittext)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_chat)

//        val send = findViewById<Button>(R.id.send_button)
        ownerFlag = intent.getBooleanExtra(TYPE, false)

        if (ownerFlag == HOST_ACTIVITY) {
            Thread(CommunicationRunnable()).start()
        } else {
            accessoryCommunication()
            gamePause(images)
        }
//        send.setOnClickListener {
//            val inputString = input.text.toString()
//            if (inputString.isNotEmpty()) {
//                sendString(inputString)
//                printLineToUI(getString(R.string.local_prompt) + inputString)
//                input.setText("")
//            }
//        }
    }


    private fun sendString(string: String?) {
        if (ownerFlag == HOST_ACTIVITY) {
            sendBuffer.add(string!!)
        } else {
            communicator!!.send(string?.toByteArray())
        }
    }


    private fun accessoryCommunication() {
        communicator = object : AccessoryCommunicator(this) {
            override fun onReceive(payload: ByteArray?, length: Int) {
                val stringReceived = String(payload!!, 0, length)
                printLineToUI("Device> $stringReceived")
                runOnUiThread {
                    gameUpdate(stringReceived)
                    gameContinue(images)
                }
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

    private fun printLineToUI(line: String) {
        runOnUiThread {
            contentTextView.text = """
     ${contentTextView.text}
     $line
     """.trimIndent()
        }
    }


    override fun onStop() {
        super.onStop()
        keepThreadAlive.set(false)
    }

    // this function will be called every time a
    // players tap in an empty box of the grid
    fun playerTap(view: View) {
        val img = view as ImageView
        val tappedImage = img.tag.toString().toInt()

        // game reset function will be called
        // if someone wins or the boxes are full
        if (!gameActive) {
            gameReset(view)
        }

        // if the tapped image is empty
        if (gameState[tappedImage] == 2) {
            // increase the counter
            // after every tap
            counter++

            // check if its the last box
            if (counter == 9) {
                // reset the game
                gameActive = false
            }

            // mark this position
            gameState[tappedImage] = activePlayer

            // this will give a motion
            // effect to the image
            img.translationY = -1000f

            // change the active player
            // 0 host and 1 guest
            // from 0 to 1 or 1 to 0
            if (activePlayer == 0) {
                // set the image of x
                img.setImageResource(R.drawable.x_png)
                activePlayer = 1
                val status = findViewById<TextView>(R.id.status)
                // change the status
                status.text = "O's Turn - Tap to play"
            } else {
                // set the image of o
                img.setImageResource(R.drawable.o_png)
                activePlayer = 0
                val status = findViewById<TextView>(R.id.status)

                // change the status
                status.text = "X's Turn - Tap to play"
            }
            sendString(img.tag.toString())
            gamePause(images)
            img.animate().translationYBy(1000f).duration = 300
        }
        var flag1 = 0
        // Check if any player has won
        for (winPosition in winPositions) {
            if (gameState[winPosition[0]] == gameState[winPosition[1]] && gameState[winPosition[1]] == gameState[winPosition[2]] && gameState[winPosition[0]] != 2) {
                flag1 = 1

                // Somebody has won! - Find out who!

                // game reset function be called
                gameActive = false
                val winnerStr: String = if (gameState[winPosition[0]] == 0) {
                    "X has won"
                } else {
                    "O has won"
                }
                // Update the status bar for winner announcement
                val status = findViewById<TextView>(R.id.status)
                status.text = winnerStr
            }
        }
        // set the status if the match draw
        if (counter == 9 && flag1 == 0) {
            val status = findViewById<TextView>(R.id.status)
            status.text = "Match Draw"
        }
    }


    private fun gameReset(view: View?) {
        gameActive = true
        activePlayer = 0
        for (i in gameState.indices) {
            gameState[i] = 2
        }
        // remove all the images from the boxes inside the grid
        for (img in images) {
            img.setImageResource(0)
        }
        val status = findViewById<TextView>(R.id.status)
        status.text = "X's Turn - Tap to play"
    }

    private fun gamePause(images: Array<ImageView>) {
        for (img in images) {
            img.isEnabled = false
        }
    }

    private fun gameContinue(images: Array<ImageView>) {
        for (img in images) {
            img.isEnabled = true
        }
    }

    private fun gameUpdate(string: String) {
        for (img in images) {
            if (img.tag.toString() == string) {
                playerTap(img)
            }
        }
    }

    private inner class CommunicationRunnable : Runnable {
        override fun run() {
            val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
            val device: UsbDevice? =
                intent.getParcelableExtra(HostConnectionActivity.DEVICE_EXTRA_KEY)
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
                    // TODO
                    //where we receive data
                    if (bytesTransferred > 0) {
                        val stringReceived = String(buff, 0, bytesTransferred)
                        printLineToUI("device> $stringReceived")
                        runOnUiThread {
                            gameUpdate(stringReceived)
                            gameContinue(images)
                        }
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

}
