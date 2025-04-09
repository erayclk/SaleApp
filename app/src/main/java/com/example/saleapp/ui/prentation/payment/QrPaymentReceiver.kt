package com.example.saleapp.ui.prentation.payment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.saleapp.model.PaymentConstants

class QrPaymentReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "QrPaymentReceiver: Broadcast received with action: ${intent.action}")
        
        if (intent.action == PaymentConstants.PAYMENT_RESPONSE_ACTION) {
            val responseCode = intent.getStringExtra(PaymentConstants.RESPONSE_CODE)
            val responseData = intent.getStringExtra(PaymentConstants.RESPONSE_DATA)
            val qrContent = intent.getStringExtra("QR_CONTENT")
            

            
            // Global bir değişkene kaydedelim
            lastQrResponse = responseData ?: ""
            lastQrContent = qrContent ?: ""
            lastResponseReceived = true
            
            // Local broadcast gönderelim
            val localIntent = Intent(ACTION_QR_PAYMENT_PROCESSED)
            localIntent.putExtra(PaymentConstants.RESPONSE_DATA, responseData)
            localIntent.putExtra("QR_CONTENT", qrContent)
            localIntent.putExtra(PaymentConstants.RESPONSE_CODE, responseCode)
            
            // Yeni Android sürümlerinde, local broadcast manager kullanımı önerilir
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(context)
                .sendBroadcast(localIntent)
            

        }
    }
    
    companion object {
        private const val TAG = "QrPaymentReceiver"
        const val ACTION_QR_PAYMENT_PROCESSED = "com.example.saleapp.QR_PAYMENT_PROCESSED"
        
        // Son yanıtları saklayan değişkenler
        var lastQrResponse = ""
        var lastQrContent = ""
        var lastResponseReceived = false
        
        fun reset() {
            lastQrResponse = ""
            lastQrContent = ""
            lastResponseReceived = false
        }
    }
} 