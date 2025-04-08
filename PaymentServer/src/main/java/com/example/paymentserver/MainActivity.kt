package com.example.paymentserver

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Date
import kotlin.concurrent.thread

// TypeConverter for Date <-> Long
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val amount: String,
    val date: Date,
    val qrContent: String? = null
)

@Dao
interface PaymentDao {
    @Insert
    suspend fun insertPayment(payment: Payment)
}

@Database(entities = [Payment::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
}

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var submitButton: Button
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputWriter: BufferedWriter? = null
    private var currentPaymentType: String = ""
    private var currentAmount: String = ""
    private var currentQRContent: String = ""
    private lateinit var database: AppDatabase
    private lateinit var paymentDao: PaymentDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "payment-db"
        ).build()
        paymentDao = database.paymentDao()

        imageView = findViewById(R.id.paymentImageView)
        submitButton = findViewById(R.id.submitButton)

        submitButton.isEnabled = false

        submitButton.setOnClickListener {
            it.isEnabled = false
            sendPaymentResponse()
        }

        startTcpServer()
    }

    private suspend fun savePaymentToDatabase() {
        val payment = Payment(
            type = currentPaymentType,
            amount = currentAmount,
            date = Date(),
            qrContent = if (currentPaymentType == "QRCode") currentQRContent else null
        )
        paymentDao.insertPayment(payment)
    }

    private fun sendPaymentResponse() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                savePaymentToDatabase()

                val responseCode = when (currentPaymentType) {
                    "Credit" -> "2"
                    "QRCode" -> "3"
                    else -> "99"
                }

                outputWriter?.write("$responseCode\n")
                outputWriter?.flush()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ödeme kaydedildi ve yanıt gönderildi: $responseCode", Toast.LENGTH_SHORT).show()
                    if (currentPaymentType == "QRCode") {
                        imageView.setImageResource(android.R.color.transparent)
                        submitButton.text = "Yeni Ödeme Bekleniyor"
                    } else {
                        imageView.setImageResource(android.R.color.transparent)
                        submitButton.text = "Yeni Ödeme Bekleniyor"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    submitButton.text = "Hata Oluştu"
                }
            } finally {
                try {
                    clientSocket?.close()
                    outputWriter = null
                } catch (ioe: Exception) {
                    ioe.printStackTrace()
                }
            }
        }
    }

    private fun startTcpServer() {
        thread {
            try {
                serverSocket = ServerSocket(9090)
                println("Server listening on port 9090")
                while (true) {
                    val acceptedSocket = serverSocket!!.accept()
                    println("Client connected: ${acceptedSocket.inetAddress}")
                    clientSocket = acceptedSocket
                    val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                    outputWriter = BufferedWriter(OutputStreamWriter(clientSocket!!.getOutputStream()))

                    val requestLine = reader.readLine()
                    if (requestLine != null) {
                        runOnUiThread { handlePaymentRequest(requestLine) }
                    } else {
                        println("Client disconnected or sent null data")
                        clientSocket?.close()
                        outputWriter = null
                    }
                }
            } catch (e: java.net.SocketException) {
                println("Server socket closed.")
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Server error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    serverSocket?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun handlePaymentRequest(requestJson: String) {
        try {
            val jsonObject = JSONObject(requestJson)
            val paymentType = jsonObject.getString("PAYMENT_TYPE")
            val amount = jsonObject.getString("AMOUNT")

            currentPaymentType = paymentType
            currentAmount = amount

            imageView.setImageResource(android.R.color.transparent)
            submitButton.isEnabled = false

            if (paymentType == "QRCode") {
                currentQRContent = jsonObject.optString("QR_CONTENT", "")
                if (currentQRContent.isNotEmpty()) {
                    generateAndDisplayQRCode(currentQRContent)
                    submitButton.text = "QR Ödemesini Onayla"
                    submitButton.isEnabled = true
                } else {
                    Toast.makeText(this, "QR İçeriği Eksik", Toast.LENGTH_SHORT).show()
                }
            } else if (paymentType == "Credit") {
                imageView.setImageResource(R.drawable.credit_card)
                Toast.makeText(this, "Kredi Kartı ile ödeme: $amount TL", Toast.LENGTH_LONG).show()
                submitButton.text = "Kredi Kartı Ödemesini Onayla"
                submitButton.isEnabled = true
            } else {
                Toast.makeText(this, "Geçersiz Ödeme Tipi: $paymentType", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "İstek işlenirken hata: ${e.message}", Toast.LENGTH_SHORT).show()
            submitButton.text = "Hata - İstek İşlenemedi"
            submitButton.isEnabled = false
        }
    }

    private fun generateAndDisplayQRCode(content: String) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val encoder = BarcodeEncoder()
            val bitmap = encoder.createBitmap(bitMatrix)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "QR Kod oluşturulamadı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        println("MainActivity onDestroy: Closing sockets.")
        try {
            outputWriter?.close()
            clientSocket?.close()
            serverSocket?.close()
            outputWriter = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
