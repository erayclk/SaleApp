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
                sendPaymentData(productId, productName, payType, payAmount, vatRate)
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

    private fun sendPaymentData(productId: Int, productName: String, payType: Int, payAmount: Double, vatRate: Int) {
        val outputStream = socket?.getOutputStream() ?: throw IllegalStateException("Socket not connected")
        
        // Ödeme tipini doğru belirle
        val paymentTypeStr = when(payType) {
            1 -> "Cash"  // Nakit ödeme
            2 -> "Credit" // Kredi kartı
            3 -> "QR"    // QR ödeme
            else -> {
                android.util.Log.w("PaymentActivity", "Unknown payment type: $payType - defaulting to Unknown")
                "Unknown"
            }
        }
        
        android.util.Log.d("PaymentActivity", "PayType from intent: $payType, converted to PaymentType: $paymentTypeStr")
        
        // Tüm verileri içeren JSON oluştur
        val paymentData = """
            {
                "ProductId": $productId,
                "ProductName": "$productName",
                "PaymentType": "$paymentTypeStr",
                "Amount": "%.2f",
                "VatRate": $vatRate
            }
        """.trimIndent().format(payAmount) + "\n"
        
        android.util.Log.d("PaymentActivity", "Sending data: $paymentData")
        outputStream.write(paymentData.toByteArray())
        outputStream.flush()
    }

    private fun handleServerResponse() {
        try {
            val inputStream = socket?.getInputStream() ?: throw IllegalStateException("Socket not connected")

            // Create buffer for response
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)

            if (bytesRead > 0) {
                val response = String(buffer, 0, bytesRead)
                android.util.Log.d("PaymentActivity", "Raw server response: $response (Bytes: $bytesRead)")

                // Parse JSON response to extract ResponseCode
                try {
                    val jsonResponse = org.json.JSONObject(response)
                    val responseCodeStr = jsonResponse.optString("ResponseCode", "")
                    
                    // Log the extracted ResponseCode
                    android.util.Log.d("PaymentActivity", "JSON ResponseCode: $responseCodeStr")
                    
                    // Make proper conversion from string code to int value
                    val responseCode = when (responseCodeStr) {
                        "00" -> 0  // Success
                        "01" -> 1  // Nakit ödeme veya QR ödeme (ProductId=1)
                        "02" -> 2  // Kredi kartı ile ödeme
                        "03" -> 3  // QR ile ödeme (diğer ProductId'ler)
                        "99" -> 99 // İptal veya Hata
                        else -> {
                            android.util.Log.w("PaymentActivity", "Unknown response code: $responseCodeStr")
                            -1 // Unknown code
                        }
                    }
                    
                    android.util.Log.d("PaymentActivity", "Determined response code: $responseCode")

                    // Ödeme türünü algılama kısmında
                    if (responseCodeStr.equals("Credit")) {
                        val amount = jsonResponse.optString("Amount", "0.00")
                        android.util.Log.d("PaymentActivity", "Detected Credit payment - Amount: $amount")
                        
                        // UI thread'de görsel güncelleme
                        runOnUiThread {
                            handleCreditPayment(amount)
                        }
                    }

                    runOnUiThread {
                        // Intent'i SaleApp'e göndermek için ayarla
                        val resultIntent = Intent().apply {
                            putExtra(PaymentConstants.RESPONSE_CODE, responseCode)
                            putExtra(PaymentConstants.RESPONSE_DATA, response)
                            putExtra("PAYMENT_COMPLETED", true)
                        }
                        
                        // Normal result için
                        android.util.Log.d("PaymentActivity", "Setting result with responseCode=$responseCode")
                        setResult(Activity.RESULT_OK, resultIntent)
                        
                        finish()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PaymentActivity", "Error parsing JSON response: ${e.message}")
                    handleError()
                }
            } else {
                android.util.Log.e("PaymentActivity", "No data received from server")
                handleError()
            }
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

    private fun handleCreditPayment(amount: String) {
        android.util.Log.d("PaymentActivity", "Handling credit payment with amount: $amount")
        // Kredi kartı işleme kodu buraya eklenebilir
    }
}