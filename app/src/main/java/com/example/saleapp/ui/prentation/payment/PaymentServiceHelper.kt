package com.example.saleapp.ui.prentation.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.saleapp.model.PaymentConstants

class PaymentServiceHelper {

    fun createCreditPaymentIntent(
        context: Context,
        productId: Int,
        productName: String,
        payAmount: Double,
        vatRate: Int
    ): Intent {
        return Intent(PaymentConstants.PAY_ACTION).apply {

            `package` = "com.example.saleapp"
            putExtra(PaymentConstants.PRODUCT_ID, productId)
            putExtra(PaymentConstants.PRODUCT_NAME, productName)
            putExtra(PaymentConstants.PAY_AMOUNT, payAmount)
            putExtra(PaymentConstants.VAT_RATE, vatRate)
            putExtra(PaymentConstants.PAY_TYPE, PaymentConstants.PAYMENT_CREDIT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    fun createQrPaymentIntent(
        context: Context,
        productId: Int,
        productName: String,
        payAmount: Double,
        vatRate: Int
    ): Intent {
        return Intent(PaymentConstants.PAY_ACTION).apply {

            `package` = "com.example.saleapp"
            putExtra(PaymentConstants.PRODUCT_ID, productId)
            putExtra(PaymentConstants.PRODUCT_NAME, productName)
            putExtra(PaymentConstants.PAY_AMOUNT, payAmount)
            putExtra(PaymentConstants.VAT_RATE, vatRate)
            putExtra(PaymentConstants.PAY_TYPE, PaymentConstants.PAYMENT_QR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}