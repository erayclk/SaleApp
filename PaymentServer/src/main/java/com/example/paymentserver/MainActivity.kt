package com.example.paymentserver

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONObject
import java.io.BufferedWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var submitButton: Button
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputWriter: BufferedWriter? = null
    private var currentPaymentType: String = ""
    private var currentAmount: String = ""
    private var currentQRContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.paymentImageView)
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            sendPaymentResponse()
        }

        startTcpServer()
    }

    private fun startTcpServer() {
        thread {
            try {
                serverSocket = ServerSocket(9090)
                
                while (true) {
                    // Her yeni bağlantıyı kabul et
                    clientSocket = serverSocket?.accept()
                    clientSocket?.let { socket ->
                        // Input ve output stream'leri ayarla
                        val inputReader = socket.getInputStream().bufferedReader()
                        outputWriter = socket.getOutputStream().bufferedWriter()
                        
                        // JSON veriyi al
                        val jsonString = inputReader.readLine()
                        if (jsonString != null) {
                            processPaymentRequest(jsonString)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processPaymentRequest(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            
            // Ödeme tipi ve detaylarını al
            currentPaymentType = jsonObject.getString("PaymentType")
            
            runOnUiThread {
                // Ödeme tipine göre uygun görüntüyü göster
                when (currentPaymentType) {
                    "Credit" -> {
                        currentAmount = jsonObject.getString("Amount")
                        showCreditCardImage()
                        Toast.makeText(this, "Kredi kartı ödemesi: $currentAmount", Toast.LENGTH_SHORT).show()
                    }
                    "QRCode" -> {
                        currentQRContent = jsonObject.getString("QRContent")
                        currentAmount = jsonObject.optString("Amount", "0")
                        showQRCode(currentQRContent)
                        Toast.makeText(this, "QR kod ödemesi", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Bilinmeyen ödeme tipi", Toast.LENGTH_SHORT).show()
                    }
                }
                
                // Submit butonu aktif hale getir
                submitButton.isEnabled = true
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "İstek işlenirken hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreditCardImage() {
        imageView.setImageResource(R.drawable.credit_card)
    }

    private fun showQRCode(content: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "QR kod oluşturulurken hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPaymentResponse() {
        runOnUiThread {
            submitButton.isEnabled = false
            Toast.makeText(this, "Ödeme onaylanıyor...", Toast.LENGTH_SHORT).show()
        }
        
        thread {
            try {
                // Ödeme tipine göre yanıt kodunu belirle
                val responseCode = when (currentPaymentType) {
                    "Credit" -> "2"
                    "QRCode" -> "3" 
                    else -> "99"
                }
                
                // SaleApp'in beklediği net yanıt formatı
                val responseJson = JSONObject().apply {
                    put("RESPONSE_TYPE", "PAYMENT_RESULT")
                    put("RESPONSE_CODE", responseCode)
                    put("PAYMENT_TYPE", currentPaymentType)
                    put("AMOUNT", currentAmount)
                    if (currentPaymentType == "QRCode") {
                        put("QR_CONTENT", currentQRContent)
                    }
                }.toString()

                // Yanıtı gönder
                outputWriter?.write(responseJson + "\n")
                outputWriter?.flush()
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, 
                        "Ödeme tamamlandı: $responseCode", 
                        Toast.LENGTH_SHORT).show()
                    
                    // QR ödemesinde ekranı kapat
                    if (currentPaymentType == "QRCode") {
                        finish() // Activity'yi sonlandır ve önceki ekrana dön
                    } else {
                        // Kredi kartı işlemlerinde ekranı sıfırla
                        imageView.setImageResource(android.R.color.transparent)
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, 
                        "Hata: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                    submitButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            outputWriter?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
