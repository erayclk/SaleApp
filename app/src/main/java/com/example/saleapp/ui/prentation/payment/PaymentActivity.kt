package com.example.saleapp.ui.prentation.payment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.saleapp.model.PaymentConstants

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Socket
import java.net.SocketTimeoutException

class PaymentActivity : ComponentActivity() {
    private var socket: Socket? = null

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
        socket = Socket("192.168.1.38", 5000)
        socket?.soTimeout = 15000 // 15 saniye timeout
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
        try {
            val inputStream = socket?.getInputStream() ?: throw IllegalStateException("Socket not connected")

            // Yanıt için buffer oluştur
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)

            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                android.util.Log.d("PaymentActivity", "Raw server response: $response (Bytes: $bytesRead)")

                runOnUiThread {
                    val resultIntent = Intent().apply {
                        putExtra(PaymentConstants.RESPONSE_CODE,
                            when {
                                response.contains("01") -> 0  // Credit success
                                response.contains("02") -> 0  // QR success
                                else -> 1  // Error
                            }
                        )
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            } else {
                android.util.Log.e("PaymentActivity", "No data received from server")
                handleError()
            }
        } catch (e: SocketTimeoutException) {
            android.util.Log.e("PaymentActivity", "Socket timeout: ${e.message}")
            handleError()
        } catch (e: Exception) {
            android.util.Log.e("PaymentActivity", "Error reading response: ${e.message}")
            handleError()
        }
    }

    private fun readFullResponse(inputStream: InputStream): String {
        val buffer = ByteArray(1024)
        val outputStream = ByteArrayOutputStream()
        var bytesRead: Int

        // Maximum 5 attempts to read data
        var attempts = 0
        var totalBytesRead = 0

        try {
            while (attempts < 5) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                // If we got a complete response, break
                if (buffer[bytesRead - 1] == '\n'.toByte()) break

                attempts++
                android.util.Log.d("PaymentActivity", "Read attempt $attempts, bytes read: $bytesRead")

                // Small delay between read attempts
                Thread.sleep(100)
            }
        } catch (e: Exception) {
            android.util.Log.e("PaymentActivity", "Error in readFullResponse: ${e.message}")
        }

        android.util.Log.d("PaymentActivity", "Total bytes read: $totalBytesRead")
        return outputStream.toString("UTF-8").trim()
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