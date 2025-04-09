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

        ioScope.launch {
            try {
                // Create initial transaction record
                val transaction = Transaction(
                    productId = productId,
                    productName = productName,
                    price = amount,
                    vatRate = vatRate,
                    status = PaymentConstants.STATUS_WAITING,
                    paymentType = paymentType
                )

                // Insert and get the ID
                /*
                val id = db.transactionDao().insert(transaction)
                android.util.Log.d("RegistryService", "Transaction inserted with ID: $id")                // Update status to completed
                db.transactionDao().updateStatusAndType(
                    id = id.toInt(),
                    status = PaymentConstants.STATUS_COMPLETED,
                    paymentType = paymentType
                )
                */
                // Verify the transaction was saved correctly
                /*
                val savedTransaction = db.transactionDao().getTransactionById(id.toInt())
                android.util.Log.d("RegistryService", """
                    Saved transaction verification:
                    ID: ${savedTransaction?.id}
                    Product ID: ${savedTransaction?.productId}
                    Product Name: ${savedTransaction?.productName}
                    Amount: ${savedTransaction?.amount}
                    Status: ${savedTransaction?.status}
                    Payment Type: ${savedTransaction?.paymentType}
                    VAT Rate: ${savedTransaction?.vatRate}
                """.trimIndent())
*/
            } catch (e: Exception) {
                android.util.Log.e("RegistryService", "Error processing payment: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}