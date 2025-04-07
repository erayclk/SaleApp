package com.example.saleapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val amount: Double,
    val status: Int, // 1: WaitsPayment, 2: PaymentCompleted
    val paymentType: Int, // 2: Credit, 3: QR
    val vatRate : Int,
    val productName : String

)