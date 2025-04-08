package com.example.saleapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.saleapp.model.PaymentConstants

import com.example.saleapp.room.TransactionDatabase
import com.example.saleapp.model.Transaction

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegistryService : Service() {

    private lateinit var db: TransactionDatabase
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = TransactionDatabase.getDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { processPayment(it) }
        return START_NOT_STICKY
    }    private fun processPayment(intent: Intent) {
        val productId = intent.getIntExtra(PaymentConstants.PRODUCT_ID, -1)
        val productName = intent.getStringExtra(PaymentConstants.PRODUCT_NAME) ?: ""
        val amount = intent.getDoubleExtra(PaymentConstants.PAY_AMOUNT, 0.0)
        val vatRate = intent.getIntExtra(PaymentConstants.VAT_RATE, 0)
        val paymentType = intent.getIntExtra(PaymentConstants.PAY_TYPE, 0)

        android.util.Log.d("RegistryService", """
            Saving transaction:
            Product ID: $productId
            Product Name: $productName
            Amount: $amount
            VAT Rate: $vatRate
            Payment Type: $paymentType
        """.trimIndent())


    }

    override fun onBind(intent: Intent?): IBinder? = null
}