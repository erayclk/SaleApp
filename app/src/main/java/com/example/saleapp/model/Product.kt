package com.example.saleapp.model

data class Product(
    val productId: Int,
    val productName: String,
    val price: Double,
    val vatRate: Int,
)
