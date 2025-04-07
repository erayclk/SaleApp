package com.example.saleapp.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.app.Service
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket

class RegistryService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("RegistryService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val productId = it.getIntExtra("productId", -1)
            val productName = it.getStringExtra("productName")
            val price = it.getDoubleExtra("price", 0.0)
            val vatRate = it.getIntExtra("vatRate", 0)

            Log.d("RegistryService", "Received product: $productId - $productName - $price - VAT: $vatRate")

            val json = JSONObject().apply {
                put("productId", productId)
                put("productName", productName)
                put("price", price)
                put("vatRate", vatRate)
            }.toString()

            Thread{
                Log.d("RegistryService", "thread start")
                sendDataToServer(json)
            }.start()


        }
        return START_NOT_STICKY
    }

    private fun sendDataToServer(json: String) {
        val serverIp = "192.168.1.38"
        val port = 5000

        try {
            val socket = Socket(serverIp, port)
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
            writer.write(json)
            writer.flush()
            writer.close()
            socket.close()
            Log.d("RegistryService", "JSON sent to registry: $json")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RegistryService", "Error sending JSON to registry: ${e.message}")
        }


    }

    override fun onBind(intent: Intent?): IBinder? =null


}