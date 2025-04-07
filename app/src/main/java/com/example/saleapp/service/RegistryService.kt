package com.example.saleapp.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.app.Service

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
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? =null


}