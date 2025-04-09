package com.example.saleapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.saleapp.model.PaymentConstants
import com.example.saleapp.room.TransactionDatabase
import com.example.saleapp.model.Transaction
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONObject
import java.io.OutputStream
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class RegistryService : Service() {

    private lateinit var db: TransactionDatabase
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val SERVER_IP = "192.168.1.38"
    private val SERVER_PORT = 5000

    override fun onCreate() {
        super.onCreate()
        db = TransactionDatabase.getDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { processPayment(it) }
        return START_NOT_STICKY
    }
    
    private fun processPayment(intent: Intent) {
        val productId = intent.getIntExtra(PaymentConstants.PRODUCT_ID, -1)
        val productName = intent.getStringExtra(PaymentConstants.PRODUCT_NAME) ?: ""
        val amount = intent.getDoubleExtra(PaymentConstants.PAY_AMOUNT, 0.0)
        val vatRate = intent.getIntExtra(PaymentConstants.VAT_RATE, 0)
        val paymentType = intent.getIntExtra(PaymentConstants.PAY_TYPE, 0)



        ioScope.launch {
            try {
                // İlk işlem kaydını oluştur
                val transaction = Transaction(
                    productId = productId,
                    productName = productName,
                    price = amount,
                    vatRate = vatRate,
                    status = PaymentConstants.STATUS_WAITING,
                    paymentType = paymentType
                )

                // QR ödeme için server'a istek gönder
                if (paymentType == PaymentConstants.PAYMENT_QR) {
                    // QR içeriğini oluştur
                    val qrContent = "product id=$productId, product name=$productName, price=$amount, vat rate = $vatRate"
                    
                    // Server'a gönderilecek JSON
                    val jsonRequest = JSONObject().apply {
                        put("PaymentType", "QR")
                        put("QRContent", qrContent)
                        
                        // Sunucunun tüm verileri göndermesi için ek alanları ekleyelim
                        put("ProductId", productId)
                        put("ProductName", productName)
                        put("Amount", amount.toString())
                        put("VatRate", vatRate)
                    }
                    
                    // Server'a istek gönder ve cevabı al
                    val serverResponse = sendQRRequestToServer(jsonRequest.toString())
                    
                    // Cevabı broadcast intent ile geri gönder
                    val responseIntent = Intent(PaymentConstants.PAYMENT_RESPONSE_ACTION).apply {
                        putExtra(PaymentConstants.RESPONSE_CODE, "03") // QR için 03 kodu
                        putExtra(PaymentConstants.RESPONSE_DATA, serverResponse)
                        putExtra("PAYMENT_COMPLETED", true)
                        putExtra("QR_CONTENT", qrContent)
                        // Intent flags ekleyelim
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                    

                }
                
                // Ekle ve Id al
                val id = db.transactionDao().insert(transaction)
                val idInt = id.toInt()

                
                // Durumu tamamlandı olarak güncelle
                db.transactionDao().updateStatusAndType(
                    id = idInt,
                    status = PaymentConstants.STATUS_COMPLETED,
                    paymentType = paymentType
                )
                
                // İşlemin doğru şekilde kaydedildiğini doğrulama
                val savedTransaction = db.transactionDao().getTransactionById(idInt)

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }
    
    private fun sendQRRequestToServer(jsonRequest: String): String {
        var response = ""
        try {
            Log.d("RegistryService", "Connecting to server $SERVER_IP:$SERVER_PORT")
            val socket = Socket(SERVER_IP, SERVER_PORT)
            
            // Request gönder
            val outputStream = socket.getOutputStream()
            outputStream.write((jsonRequest + "\n").toByteArray())
            outputStream.flush()
            
            Log.d("RegistryService", "Request sent: $jsonRequest")
            
            // Cevabı al
            val inputStream = socket.getInputStream()
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            
            if (bytesRead > 0) {
                response = String(buffer, 0, bytesRead)
                Log.d("RegistryService", "Response received: $response")
            }
            
            socket.close()
        } catch (e: Exception) {
            Log.e("RegistryService", "Error communicating with server: ${e.message}")
            // Hata durumunda basit bir yanıt döndür
            response = """{"ResponseCode":"03","ErrorMessage":"${e.message}"}"""
        }
        
        return response
    }

    override fun onBind(intent: Intent?): IBinder? = null
}