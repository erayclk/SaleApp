package com.example.saleapp.ui.prentation.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.saleapp.model.PaymentConstants

class PaymentActivity : ComponentActivity() {
    private var socket: java.net.Socket? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        processPayment()
    }
    
    private fun processPayment() {
        // Get payment details from intent
        val productId = intent.getIntExtra(PaymentConstants.PRODUCT_ID, -1)
        val productName = intent.getStringExtra(PaymentConstants.PRODUCT_NAME) ?: ""
        val payAmount = intent.getDoubleExtra(PaymentConstants.PAY_AMOUNT, 0.0)
        val vatRate = intent.getIntExtra(PaymentConstants.VAT_RATE, 0)
        val payType = intent.getIntExtra(PaymentConstants.PAY_TYPE, 0)

        android.util.Log.d("PaymentActivity", """
            Payment Details:
            Product ID: $productId
            Product Name: $productName
            Amount: $payAmount
            VAT Rate: $vatRate
            Pay Type: $payType
        """.trimIndent())

        Thread {
            try {
                connectToServer()
                sendPaymentData(payType, payAmount)
                handleServerResponse()
            } catch (e: Exception) {
                android.util.Log.e("PaymentActivity", "Error during payment: ${e.message}")
                e.printStackTrace()
                handleError()
            } finally {
                closeConnection()
            }
        }.start()
    }

    private fun connectToServer() {
        android.util.Log.d("PaymentActivity", "Attempting to connect to server...")
        socket = java.net.Socket("192.168.1.38", 5000)
        android.util.Log.d("PaymentActivity", "Connected to server")
    }

    private fun sendPaymentData(payType: Int, payAmount: Double) {
        val outputStream = socket?.getOutputStream() ?: throw IllegalStateException("Socket not connected")
        val paymentData = """{"PaymentType":"${if (payType == 2) "Credit" else "QR"}","Amount":"%.2f"}\n""".format(payAmount)
        android.util.Log.d("PaymentActivity", "Sending data: $paymentData")
        outputStream.write(paymentData.toByteArray())
        outputStream.flush()
    }

    private fun handleServerResponse() {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(socket?.getInputStream()))
        var response: String? = null
        
        val timeoutThread = Thread {
            //Thread.sleep(5000)
            if (response == null) {
                socket?.close()
            }
        }
        timeoutThread.start()

        response = reader.readLine()
        android.util.Log.d("PaymentActivity", "Server response: $response")
        timeoutThread.interrupt()

        runOnUiThread {
            val resultIntent = Intent().apply {
                putExtra(PaymentConstants.RESPONSE_CODE, if (response?.contains("00") == true) 0 else 1)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun handleError() {
        runOnUiThread {
            val resultIntent = Intent().apply {
                putExtra(PaymentConstants.RESPONSE_CODE, 99)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun closeConnection() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            android.util.Log.e("PaymentActivity", "Error closing socket: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
    }
}
