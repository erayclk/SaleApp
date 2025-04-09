package com.example.saleapp.model

object PaymentConstants {

    // Actions
    const val PAY_ACTION = "com.example.registry.PAY_ACTION"
    const val PAYMENT_RESPONSE_ACTION = "com.example.saleapp.PAYMENT_RESPONSE"

    // Data keys
    const val PRODUCT_ID = "PRODUCT_ID"
    const val PRODUCT_NAME = "PRODUCT_NAME"
    const val PAY_AMOUNT = "PAY_AMOUNT"
    const val VAT_RATE = "VAT_RATE"
    const val PAY_TYPE = "PAY_TYPE"
    const val RESPONSE_CODE = "RESPONSE_CODE"
    const val RESPONSE_DATA = "response_data"
    // Payment types
    const val PAYMENT_CASH = 1
    const val PAYMENT_CREDIT = 2
    const val PAYMENT_QR = 3

    // Status codes
    const val STATUS_WAITING = 1
    const val STATUS_COMPLETED = 2
}
