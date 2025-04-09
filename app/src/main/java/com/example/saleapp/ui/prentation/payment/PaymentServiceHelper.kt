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
        val intent = Intent(context, PaymentActivity::class.java)
        intent.apply {
            putExtra(PaymentConstants.PRODUCT_ID, productId)
            putExtra(PaymentConstants.PRODUCT_NAME, productName)
            putExtra(PaymentConstants.PAY_AMOUNT, payAmount)
            putExtra(PaymentConstants.VAT_RATE, vatRate)
            putExtra(PaymentConstants.PAY_TYPE, PaymentConstants.PAYMENT_CREDIT)
        }
        return intent
    }
}