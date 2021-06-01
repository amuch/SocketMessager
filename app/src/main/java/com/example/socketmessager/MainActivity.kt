package com.example.socketmessager

import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity(), View.OnClickListener {
    var messageHandler: Handler? = null
    private var mainTextView: TextView? = null
    private var sourceTextView:TextView? = null
    private var destinationTextView:TextView? = null
    private var messageEditText: EditText? = null
    private var hostEditText:EditText? = null
    private var portEditText:EditText? = null
    private var socketThread: SocketThread? = null
    private var lines = 0
    private var clientPort:kotlin.Int = 0
    private var host: String? = null
    private var client:kotlin.String? = null
    private var send = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindUI()

        try {
            host = getLocalIpAddress()
            println("Host is : $host")
            sourceTextView!!.text = host
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        val port = 8080
        messageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                val bundle = message.data
                val msg = bundle.getString("message")
                val rec = bundle.getLong("receivedTime")
                lines++
                if (lines > 20) {
                    mainTextView!!.text = "$msg $rec"
                    lines = 0
                } else {
                    mainTextView!!.append("\n$msg $rec")
                }
            }
        }
        Thread(Runnable {
            val handler: Handler? = messageHandler
            val serverSocket = ServerSocket(port)
            while (true) {
                val socket = serverSocket.accept()
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = input.readLine()
                val message = Message()
                val bundle = Bundle()
                bundle.putString("message", line)
                val receivedTime = System.currentTimeMillis()
                bundle.putLong("receivedTime", receivedTime)
                message.data = bundle
                handler!!.sendMessage(message)
                input.close()
                socket.close()
            }
        }).start()
//        socketThread = SocketThread(host, port, messageHandler)
//        socketThread!!.start()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.socket_connect_button -> connectSocket()
            R.id.socket_message_button -> sendMessage()
        }
    }

    private fun connectSocket() {
        client = hostEditText!!.text.toString()
        val port = portEditText!!.text.toString()
        if (client != null && port != null) {
            clientPort = port.toInt()
            destinationTextView!!.text = "$client:$clientPort"
            send = true
        }
    }

    private fun sendMessage() {
        val message = messageEditText!!.text.toString()
        if (send && message != null) {
            val sent = System.currentTimeMillis()
            lines++
            mainTextView!!.append(
                """
                
                $sent
                """.trimIndent()
            )
            Thread(Runnable {
                val socket: Socket
                val printWriter: PrintWriter
                try {
                    socket = Socket(client!!, clientPort)
                    printWriter = PrintWriter(socket.getOutputStream())
                    printWriter.write(message)
                    printWriter.flush()
                    printWriter.close()
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }).start()
        }
    }

    @Throws(UnknownHostException::class)
    private fun getLocalIpAddress(): String? {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }

    private fun bindUI() {
        lines = 0
        send = false
        mainTextView = findViewById(R.id.main_text_view)
        sourceTextView = findViewById(R.id.source_ip_text_view)
        destinationTextView = findViewById(R.id.destination_ip_text_view)

        messageEditText = findViewById(R.id.socket_message_edit_text)
        hostEditText = findViewById(R.id.socket_host_edit_text)
        portEditText = findViewById(R.id.socket_port_edit_text)

        val connect = findViewById<Button>(R.id.socket_connect_button)
        connect.setOnClickListener(this)

        val message = findViewById<Button>(R.id.socket_message_button)
        message.setOnClickListener(this)
    }


}