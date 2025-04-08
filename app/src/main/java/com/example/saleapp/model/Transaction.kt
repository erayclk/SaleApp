package com.example.saleapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val price: Double,
    val vatRate: Int,
    val status: Int = 0,
    val paymentType: Int = -1
)